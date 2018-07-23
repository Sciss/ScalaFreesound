/*
 *  RetrievalViewImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound
package lucre
package impl

import java.awt.Toolkit
import java.awt.geom.Path2D
import javax.swing.{Icon, JComponent, KeyStroke, Timer}

import de.sciss.audiowidgets.Transport
import de.sciss.audiowidgets.Transport.{ButtonStrip, Loop, Play, Stop}
import de.sciss.file._
import de.sciss.freesound.swing.{SearchView, Shapes, SoundTableView, SoundView}
import de.sciss.icons.raphael
import de.sciss.lucre.stm
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{deferTx, requireEDT}
import de.sciss.lucre.synth.{Buffer, Server, Synth, Sys}
import de.sciss.synth.proc.{AuralSystem, SoundProcesses}
import de.sciss.synth.{ControlSet, SynthGraph}

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.Future
import scala.concurrent.stm.Ref
import scala.swing.event.Key
import scala.swing.{AbstractButton, Action, BorderPanel, BoxPanel, Button, Component, Orientation, Panel, SequentialContainer, Swing, TabbedPane}
import scala.util.Success
import scala.util.control.NonFatal

object RetrievalViewImpl {
  def apply[S <: Sys[S]](searchInit: TextSearch, soundInit: ISeq[Sound])
           (implicit tx: S#Tx, client: Client, previewsCache: PreviewsCache,
            aural: AuralSystem, cursor: stm.Cursor[S]): RetrievalView[S] = {
    new Impl[S](searchInit, soundInit).init()
  }

  private def iconNormal  (fun: Path2D => Unit): Icon = raphael.TexturedIcon        (20)(fun)
  private def iconDisabled(fun: Path2D => Unit): Icon = raphael.TexturedDisabledIcon(20)(fun)

  private def toolButton(action: Action, iconFun: Path2D => Unit, tooltip: String): Button = {
    val res           = new Button(action)
    res.peer.putClientProperty("styleId", "icon-space")
    res.icon          = iconNormal  (iconFun)
    res.disabledIcon  = iconDisabled(iconFun)
    // res.peer.putClientProperty("JButton.buttonType", "textured")
    if (!tooltip.isEmpty) res.tooltip = tooltip
    res
  }

  private def addGlobalKeyWhenVisible(b: AbstractButton, keyStroke: KeyStroke): Unit = {
    val click = Action(null) {
      if (b.showing) b.doClick()
    }
    b.peer.getActionMap.put("click", click.peer)
    b.peer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "click")
  }

  private def addGlobalAction(c: Component, name: String, keyStroke: KeyStroke)(body: => Unit): Unit = {
    val a = Action(null)(body)
    c.peer.getActionMap.put(name, a.peer)
    c.peer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name)
  }

  private final class Impl[S <: Sys[S]](searchInit: TextSearch, soundInit: ISeq[Sound])
                                       (implicit client: Client, previewCache: PreviewsCache,
                                        aural: AuralSystem, val cursor: stm.Cursor[S])
    extends RetrievalView[S] with ComponentHolder[Component] {

    type C = Component

    def init()(implicit tx: S#Tx): this.type = {
      deferTx(guiInit())
      this
    }

    private[this] val _disposed = Ref(false)

    def dispose()(implicit tx: S#Tx): Unit = {
      _disposed() = true
      stopAndRelease()
    }

    private def stopAndRelease()(implicit tx: S#Tx): Unit = {
      synth   .swap(None).foreach(_.dispose())
      acquired.swap(None).foreach { sound =>
        // XXX TODO - workaround for https://github.com/Sciss/FileCache/issues/5
        try {
          previewCache.release(sound)
        } catch {
          case NonFatal(_) => // ignored
        }
      }
      deferTx(timerPrepare.stop())
    }

    private[this] var selected = ISeq.empty[Sound]

    private def updateSelection(xs: ISeq[Sound]): Unit = {
      selected        = xs
      val isSingle    = xs.size == 1
      val ggPlay      = transPane.button(Play).get
      ggPlay.enabled  = isSingle
      ggView.enabled  = xs.nonEmpty
    }

    private[this] val acquired  = Ref(Option.empty[Sound])
    private[this] val synth     = Ref(Option.empty[Synth])
    private[this] val isLooping = Ref(true)

    private[this] var timerPrepare: Timer = _
    private[this] var transPane   : Component with ButtonStrip = _
    private[this] var ggView      : Button = _

    private def play(s: Server, f: File, sampleRate: Double, numChannels: Int)(implicit tx: S#Tx): Unit = {
      val buf = Buffer.diskIn(s)(path = f.path, startFrame = 0L, numChannels = numChannels)
      val graph = SynthGraph {
        import de.sciss.synth.Ops.stringToControl
        import de.sciss.synth.ugen._
        val speed = "speed".ir
        val bufId = "buf"  .ir
        val loop  = "loop" .kr
        val disk  = VDiskIn.ar(numChannels = numChannels, buf = bufId, speed = speed, loop = loop)
//        val done  = Done.kr(disk)
//        FreeSelf.kr(done)
        val sig   = if (numChannels == 1) Pan2.ar(disk) else disk
        Out.ar(0, sig)
      }
      val speed = sampleRate.toDouble / s.sampleRate
      val loopI = if (isLooping()) 1f else 0f
      val args  = List[ControlSet]("speed" -> speed, "buf" -> buf.id, "loop" -> loopI)
      val syn   = Synth.play(graph, nameHint = Some(s"preview-$numChannels"))(
        target = s.defaultGroup, args = args, dependencies = buf :: Nil)
      syn.onEndTxn { implicit tx => buf.dispose() }
      synth.swap(Some(syn)).foreach(_.dispose())
    }

    private def markT(element: Transport.Element): Unit = {
      val gg = transPane.button(element).get
      gg.selected = true
    }

    private def unmarkT(element: Transport.Element): Unit = {
      val gg = transPane.button(element).get
      gg.selected = false
    }

    private def toggleT(element: Transport.Element): Unit = {
      val gg = transPane.button(element).get
      gg.selected = !gg.selected
      Toolkit.getDefaultToolkit.sync()
    }

    private[this] var _searchView     : SearchView      = _
    private[this] var _soundTableView : SoundTableView  = _
    private[this] var _soundView      : SoundView       = _
    private[this] var _bottomPane     : BoxPanel        = _
    private[this] var _tabs           : TabbedPane      = _

    private[this] var pageSearch  : TabbedPane.Page = _
    private[this] var pageResults : TabbedPane.Page = _
    private[this] var pageInfo    : TabbedPane.Page = _

    def resultBottomComponent: Panel with SequentialContainer = _bottomPane

    def tabbedPane: TabbedPane = _tabs

    def searchView: SearchView = {
      requireEDT()
      _searchView
    }

    def soundTableView: SoundTableView = {
      requireEDT()
      _soundTableView
    }

    def soundView: SoundView = {
      requireEDT()
      _soundView
    }

    def showSearch  (): Unit = _tabs.selection.page = pageSearch
    def showResults (): Unit = _tabs.selection.page = pageResults
    def showInfo    (): Unit = _tabs.selection.page = pageInfo

    def search: TextSearch = TextSearch(
      query = _searchView.query, filter = _searchView.filter, sort =_searchView.sort,
      groupByPack = _searchView.groupByPack, maxItems = _searchView.maxItems)

    private def guiInit(): Unit = {
      _searchView       = SearchView    ()
      _soundTableView   = SoundTableView()
      _soundView        = SoundView     ()

      _searchView.query       = searchInit.query
      _searchView.groupByPack = searchInit.groupByPack
      _searchView.maxItems    = searchInit.maxItems
      _searchView.sort        = searchInit.sort

      if (searchInit.filter.nonEmpty) _searchView    .filter = searchInit.filter
      if (soundInit        .nonEmpty) _soundTableView.sounds = soundInit

//      _searchView.previews = true

//      def previewRtz(): Unit = {
//        val isPlaying = synth.single().isDefined
//        previewStop()
//        if (isPlaying) previewPlay()
//      }

      def previewStop(): Unit = {
        cursor.step { implicit tx =>
          stopAndRelease()
        }
        unmarkT(Play)
        markT  (Stop)
      }

      def previewPlay(): Unit = selected match {
        case ISeq(single) => previewPlay1(single)
        case _ =>
      }

      def previewPlay1(sound: Sound): Unit = {
        unmarkT(Stop)

        val fut = cursor.step { implicit tx =>
          stopAndRelease()
          // XXX TODO - workaround for https://github.com/Sciss/FileCache/issues/5
          try {
            val _fut = previewCache.acquire(sound)
            acquired() = Some(sound)
            _fut
          } catch {
            case NonFatal(ex) =>
              Future.failed(ex)
          }
        }

        timerPrepare.restart()

        import previewCache.executionContext
        fut.onComplete { tr =>
          SoundProcesses.atomic[S, Unit] { implicit tx =>
            if (acquired().contains(sound) && !_disposed()) {
              deferTx(timerPrepare.stop())
              (tr, aural.serverOption) match {
                case (Success(f), Some(s)) =>
                  val sampleRate  = sound.sampleRate                // this seems to be preserved
                  val numChannels = math.min(2, sound.numChannels)  // this seems to be constrained
                  play(s, f, sampleRate = sampleRate, numChannels = numChannels)
                  deferTx(markT(Play))

                case _ =>
                  deferTx {
                    unmarkT(Play)
                    markT  (Stop)
                  }
              }
            }
          }
        }
      }

      def previewLoop(): Unit = {
        val gg = transPane.button(Loop).get
        val value = !gg.selected
        gg.selected = value
        cursor.step { implicit tx =>
          isLooping() = value
          val valueI  = if (value) 1f else 0f
          synth().foreach(_.set("loop" -> valueI))
        }
      }

      transPane = {
        Transport.makeButtonStrip(
          Seq(/* GoToBegin(previewRtz()), */ Stop(previewStop()), Play(previewPlay()), Loop(previewLoop())))
      }
      markT(Loop)

      timerPrepare = new Timer(100, Swing.ActionListener { _ =>
        toggleT(Play)
      })
      timerPrepare.setRepeats(true)

//      val axis          = new Axis
//      axis.fixedBounds  = true
//      axis.format       = AxisFormat.Time()

      pageInfo    = new TabbedPane.Page("Sound"  , _soundView.component , null)

      def viewSounds(): Unit = selected match {
        case ISeq(single) =>
          _soundView.sound = Some(single)
          showInfo()
        case _ =>
      }

      ggView = toolButton(Action(null)(viewSounds()), Shapes.SoundInfo /* raphael.Shapes.View */, tooltip = "View Sound Information")
      val menu1 = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
      addGlobalKeyWhenVisible(ggView, KeyStroke.getKeyStroke(Key.I.id, menu1))

      _bottomPane = new BoxPanel(Orientation.Horizontal) {
        contents += Swing.HStrut(4)
        contents += ggView
        contents += Swing.HStrut(4)
        contents += transPane
        contents += Swing.HStrut(4)
      }
      val box = new BoxPanel(Orientation.Vertical) {
//        contents += axis
        contents += Swing.VStrut(4)
        contents += _bottomPane
      }

      val resultsPane = new BorderPanel {
        add(_soundTableView.component, BorderPanel.Position.Center)
        add(box              , BorderPanel.Position.South )
      }

      _tabs        = new TabbedPane
      _tabs.peer.putClientProperty("styleId", "attached")
      _tabs.focusable  = false
      pageSearch  = new TabbedPane.Page("Search" , _searchView.component, null)
      pageResults = new TabbedPane.Page("Results", resultsPane          , null)
      _tabs.pages     += pageSearch
      _tabs.pages     += pageResults
      _tabs.pages     += pageInfo

      addGlobalAction(_tabs, "prev", KeyStroke.getKeyStroke(Key.Left.id, Key.Modifier.Alt)) {
        val sel   = _tabs.selection
        val idx   = sel.index - 1
        sel.index = if (idx >= 0) idx else _tabs.pages.size - 1
      }
      addGlobalAction(_tabs, "next", KeyStroke.getKeyStroke(Key.Right.id, Key.Modifier.Alt)) {
        val sel   = _tabs.selection
        val idx   = sel.index + 1
        sel.index = if (idx < _tabs.pages.size) idx else 0
      }
      addGlobalAction(_tabs, "play-stop", KeyStroke.getKeyStroke(Key.Space.id, 0)) {
        if (transPane.showing) {
          if (synth.single.get.isDefined) previewStop() else previewPlay()
        }
      }

      _searchView.addListener {
        case SearchView.SearchResult(_, _, Success(xs)) =>
          _soundTableView.sounds = xs
          showResults()
      }

      _soundTableView.addListener {
        case SoundTableView.Selection(xs) => updateSelection(xs)
      }

      component = _tabs
    }
  }
}