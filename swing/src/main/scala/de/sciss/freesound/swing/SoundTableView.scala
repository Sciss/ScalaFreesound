/*
 *  SoundTableView.scala
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
package swing

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.{Component, Table}

object SoundTableView {
  def apply(): SoundTableView = impl.SoundTableViewImpl()
}
trait SoundTableView {
  /** The top level component containing the view. */
  def component: Component

  /** The table component (which is not the top-level component!).
    * Use this to customise the table, install a selection listener, etc.
    */
  def table: Table

  /** Get or set the current list of sound instances shown in the table. */
  var sounds: ISeq[Sound]

//  /** Get or set the current list of sound instances selected in the table. */
//  var selectedSounds: ISeq[Sound]


}