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
  def apply(init: ISeq[Sound] = Nil): SoundTableView = impl.SoundTableViewImpl(init)
}
trait SoundTableView {
  def component: Component

  def table: Table

  var sounds: ISeq[Sound]
}