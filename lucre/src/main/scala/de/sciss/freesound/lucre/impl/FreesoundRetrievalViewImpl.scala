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

import de.sciss.file.File
import de.sciss.filecache.MutableConsumer
import de.sciss.freesound.swing.{FilterView, SoundTableView}

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.{Component, TabbedPane}

object FreesoundRetrievalViewImpl {
  def apply(queryInit: String, filterInit: Filter, soundInit: ISeq[Sound])
           (implicit client: Client, previewCache: MutableConsumer[Int, File]): FreesoundRetrievalView = {
    new Impl(filterInit, soundInit)
  }

  private final class Impl(filterInit: Filter, soundInit: ISeq[Sound])
                          (implicit client: Client, previewCache: MutableConsumer[Int, File])
    extends FreesoundRetrievalView {

    private[this] val filterView      = FilterView    (filterInit)
    private[this] val soundTableView  = SoundTableView(soundInit )

    lazy val component: Component = {
      val tabs = new TabbedPane
      tabs.pages += new TabbedPane.Page("Search" , ???)
      tabs.pages += new TabbedPane.Page("Results", ???)
      ???
    }
  }
}
