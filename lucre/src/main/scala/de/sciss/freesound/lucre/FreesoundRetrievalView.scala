/*
 *  FreesoundRetrievalView.scala
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

import de.sciss.file.File
import de.sciss.filecache.MutableConsumer
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.View

import scala.swing.Component

object FreesoundRetrievalView {
  def apply(queryInit: String = "", filterInit: Filter = Filter())
           (implicit client: Client, previewCache: MutableConsumer[Int, File]): FreesoundRetrievalView =
    ???
}
trait FreesoundRetrievalView {
  def component: Component
}