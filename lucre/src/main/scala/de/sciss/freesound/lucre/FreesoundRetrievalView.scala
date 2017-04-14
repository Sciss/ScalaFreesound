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

import java.net.URI

import de.sciss.file.File
import de.sciss.filecache.MutableConsumer

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.Component

object FreesoundRetrievalView {
  def apply(queryInit: String = "", filterInit: Filter = Filter(), soundInit: ISeq[Sound] = Nil)
           (implicit client: Client, previewCache: MutableConsumer[URI, File]): FreesoundRetrievalView =
    impl.FreesoundRetrievalViewImpl(queryInit = queryInit, filterInit = filterInit, soundInit = soundInit)
}
trait FreesoundRetrievalView {
  def component: Component
}