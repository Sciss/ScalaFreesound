/*
 *  FilterView.scala
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

package de.sciss.freesound.swing

import de.sciss.freesound.Filter
import de.sciss.model.Model

import scala.swing.Component

object FilterView {
  def apply(init: Filter = Filter()): FilterView = impl.FilterViewImpl(init)
}

/** A view with a component that contains widget for editing a filter description.
  */
trait FilterView extends Model[Filter] {
  def component: Component

  var filter: Filter
}
