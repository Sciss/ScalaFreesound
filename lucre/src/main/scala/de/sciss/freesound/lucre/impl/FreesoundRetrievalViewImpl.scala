/*
 *  FreesoundRetrievalViewImpl.scala
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
import javax.swing.Timer

import de.sciss.audiowidgets.Transport
import de.sciss.audiowidgets.Transport.{ButtonStrip, Loop, Play, Stop}
import de.sciss.file._
import de.sciss.freesound.swing.{SearchView, SoundTableView}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.swing.{deferTx, requireEDT}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.synth.{Buffer, Server, Synth, Sys}
import de.sciss.synth.proc.{AuralSystem, SoundProcesses}
import de.sciss.synth.{ControlSet, SynthGraph}

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.stm.Ref
import scala.swing.{BorderPanel, BoxPanel, Component, Orientation, Swing, TabbedPane}
import scala.util.Success

object FreesoundRetrievalViewImpl {
  def apply[S <: Sys[S]](queryInit: String, filterInit: Filter, soundInit: ISeq[Sound])
           (implicit tx: S#Tx, client: Client, previewCache: PreviewsCache,
            aural: AuralSystem, cursor: stm.Cursor[S]): FreesoundRetrievalView[S] = {
    new Impl[S](queryInit, filterInit, soundInit).init()
  }

  private final class Impl[S <: Sys[S]](queryInit: String, filterInit: Filter, soundInit: ISeq[Sound])
                                       (implicit client: Client, previewCache: PreviewsCache,
                                        aural: AuralSystem, val cursor: stm.Cursor[S])
    extends FreesoundRetrievalView[S] with ComponentHolder[Component] {

    def init()(implicit tx: S#Tx): this.type = {
      deferTx(guiInit())
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      stopAndRelease()
    }

    private def stopAndRelease()(implicit tx: S#Tx): Unit = {
      synth   .swap(None).foreach(_.dispose())
      acquired.swap(None).foreach(previewCache.release)
      deferTx(timerPrepare.stop())
    }

    private[this] var selected = Option.empty[Sound]

    private def selectionUpdated(): Unit = ()

    private[this] val acquired  = Ref(Option.empty[Previews])
    private[this] val synth     = Ref(Option.empty[Synth   ])
    private[this] val isLooping = Ref(true)

    private[this] var timerPrepare: Timer = _
    private[this] var transPane   : Component with ButtonStrip = _

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

    def searchView: SearchView = {
      requireEDT()
      _searchView
    }

    def soundTableView: SoundTableView = {
      requireEDT()
      _soundTableView
    }

    private def guiInit(): Unit = {
      _searchView      = SearchView    ()
      _soundTableView  = SoundTableView()

      if (queryInit .nonEmpty) _searchView    .query  = queryInit
      if (filterInit.nonEmpty) _searchView    .filter = filterInit
      if (soundInit .nonEmpty) _soundTableView.sounds = soundInit

      _searchView.previews = true

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

      def previewPlay(): Unit =
        for {
          sound    <- selected
          previews <- sound.previews
        } {
          unmarkT(Stop)

          val fut = cursor.step { implicit tx =>
            stopAndRelease()
            acquired() = Some(previews)
            previewCache.acquire(previews)
          }
          import SoundProcesses.executionContext

          timerPrepare.restart()
          fut.onComplete { tr =>
            cursor.step { implicit tx =>
              if (acquired().contains(previews)) {
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

      val bottomPane = new BoxPanel(Orientation.Vertical) {
//        contents += axis
        contents += Swing.VStrut(4)
        contents += transPane
      }

      val resultsPane = new BorderPanel {
        add(_soundTableView.component, BorderPanel.Position.Center)
        add(bottomPane              , BorderPanel.Position.South )
      }

      val tabs        = new TabbedPane
      tabs.peer.putClientProperty("styleId", "attached")
      tabs.focusable  = false
      val pageSearch  = new TabbedPane.Page("Search" , _searchView.component)
      val pageResults = new TabbedPane.Page("Results", resultsPane)
      tabs.pages     += pageSearch
      tabs.pages     += pageResults

      _searchView.addListener {
        case SearchView.SearchResult(_, _, Success(xs)) =>
          _soundTableView.sounds = xs
          tabs.selection.page   = pageResults
      }

      _soundTableView.addListener {
        case SoundTableView.Selection(xs) =>
          selected = xs match {
            case Seq(x) => Some(x)
            case _      => None
          }
          selectionUpdated()
      }

      component = tabs
    }
  }
}