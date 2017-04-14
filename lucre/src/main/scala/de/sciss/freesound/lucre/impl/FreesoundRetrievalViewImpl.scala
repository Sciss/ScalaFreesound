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

import de.sciss.audiowidgets.{Axis, AxisFormat, Transport}
import de.sciss.freesound.swing.{SearchView, SoundTableView}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.swing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.synth.{Synth, Sys}
import de.sciss.synth.proc.AuralSystem
import de.sciss.synth.proc.SoundProcesses

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.stm.Ref
import scala.swing.{BorderPanel, BoxPanel, Component, Orientation, TabbedPane}
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
    }

    private[this] var selected = Option.empty[Sound]

    private def selectionUpdated(): Unit = ()

    private[this] val acquired  = Ref(Option.empty[Previews])
    private[this] val synth     = Ref(Option.empty[Synth   ])

    private def guiInit(): Unit = {
      val searchView      = SearchView    ()
      val soundTableView  = SoundTableView()

      if (queryInit .nonEmpty) searchView    .query  = queryInit
      if (filterInit.nonEmpty) searchView    .filter = filterInit
      if (soundInit .nonEmpty) soundTableView.sounds = soundInit

      searchView.previews = true

      def previewRtz(): Unit = {

      }

      def previewStop(): Unit = {

      }

      def previewPlay(): Unit =
        for {
          sound    <- selected
          previews <- sound.previews
        } {
          val fut = cursor.step { implicit tx =>
            stopAndRelease()
            acquired() = Some(previews)
            previewCache.acquire(previews)
          }
          import SoundProcesses.executionContext
          fut.foreach { f =>
            println(s"Yo, got $f")
          }
        }

      def previewLoop(): Unit = {

      }

      val transPane = {
        import Transport._
        makeButtonStrip(
          Seq(GoToBegin(previewRtz()), Stop(previewStop()), Play(previewPlay()), Loop(previewLoop())))
      }

      val axis          = new Axis
      axis.fixedBounds  = true
      axis.format       = AxisFormat.Time()

      val bottomPane = new BoxPanel(Orientation.Vertical) {
        contents += axis
        contents += transPane
      }

      val resultsPane = new BorderPanel {
        add(soundTableView.component, BorderPanel.Position.Center)
        add(bottomPane              , BorderPanel.Position.South )
      }

      val tabs        = new TabbedPane
      tabs.peer.putClientProperty("styleId", "attached")
      tabs.focusable  = false
      val pageSearch  = new TabbedPane.Page("Search" , searchView.component)
      val pageResults = new TabbedPane.Page("Results", resultsPane)
      tabs.pages     += pageSearch
      tabs.pages     += pageResults

      searchView.addListener {
        case SearchView.SearchResult(_, _, Success(xs)) =>
          soundTableView.sounds = xs
          tabs.selection.page   = pageResults
      }

      soundTableView.addListener {
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