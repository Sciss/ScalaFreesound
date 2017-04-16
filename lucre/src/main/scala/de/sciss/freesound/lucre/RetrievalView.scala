/*
 *  RetrievalView.scala
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

import de.sciss.freesound.swing.{SearchView, SoundTableView}
import de.sciss.lucre.stm
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Sys
import de.sciss.synth.proc.AuralSystem

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.{Component, Panel, SequentialContainer, TabbedPane}

object RetrievalView {
  def apply[S <: Sys[S]](searchInit: TextSearch, soundInit: ISeq[Sound] = Nil)
           (implicit tx: S#Tx, client: Client, previewsCache: PreviewsCache,
            aural: AuralSystem, cursor: stm.Cursor[S]): RetrievalView[S] =
    impl.RetrievalViewImpl[S](searchInit = searchInit, soundInit = soundInit)
}
trait RetrievalView[S <: stm.Sys[S]] extends View.Cursor[S] {
  /** Swing view; must call on EDT! */
  def searchView    : SearchView

  /** Swing view; must call on EDT! */
  def soundTableView: SoundTableView

  /** Swing view -- clients can append their own components. */
  def resultBottomComponent: Component with SequentialContainer

  /** Swing view; must call on EDT! */
  def tabbedPane: TabbedPane

  def search: TextSearch
}