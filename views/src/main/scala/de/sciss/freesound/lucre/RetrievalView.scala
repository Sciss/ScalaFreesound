/*
 *  RetrievalView.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.lucre

import de.sciss.freesound.swing.{SearchView, SoundTableView, SoundView}
import de.sciss.freesound.{Client, Sound, TextSearch}
import de.sciss.lucre.{Txn => LTxn}
import de.sciss.lucre.synth.Txn
import de.sciss.mellite.UniverseView
import de.sciss.proc.Universe

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.{Component, SequentialContainer, TabbedPane}

object RetrievalView {
  def apply[T <: Txn[T]](searchInit: TextSearch, soundInit: ISeq[Sound] = Nil)
           (implicit tx: T, client: Client, previewsCache: PreviewsCache,
            universe: Universe[T]): RetrievalView[T] =
    impl.RetrievalViewImpl[T](searchInit = searchInit, soundInit = soundInit)
}
trait RetrievalView[T <: LTxn[T]] extends UniverseView[T] {
  /** Swing view; must call on EDT! */
  def searchView    : SearchView

  /** Swing view; must call on EDT! */
  def soundTableView: SoundTableView

  /** Swing view; must call on EDT! */
  def soundView: SoundView

  /** Swing view -- clients can append their own components. */
  def resultBottomComponent: Component with SequentialContainer

  /** Swing view; must call on EDT! */
  def tabbedPane: TabbedPane

  def search: TextSearch

  /** Shows the tab for the search settings. */
  def showSearch    (): Unit

  /** Shows the tab for the search results. */
  def showResults   (): Unit

  /** Shows the tab for the sound information. */
  def showInfo      (): Unit
}