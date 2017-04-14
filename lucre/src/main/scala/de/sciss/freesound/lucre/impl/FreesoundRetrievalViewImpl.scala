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

import java.net.URI

import de.sciss.file.File
import de.sciss.filecache.MutableConsumer
import de.sciss.freesound.swing.{SearchView, SoundTableView}

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.{Component, TabbedPane}

object FreesoundRetrievalViewImpl {
  def apply(queryInit: String, filterInit: Filter, soundInit: ISeq[Sound])
           (implicit client: Client, previewCache: MutableConsumer[URI, File]): FreesoundRetrievalView = {
    new Impl(queryInit, filterInit, soundInit)
  }

  private final class Impl(queryInit: String, filterInit: Filter, soundInit: ISeq[Sound])
                          (implicit client: Client, previewCache: MutableConsumer[URI, File])
    extends FreesoundRetrievalView {

    private[this] val searchView      = SearchView    ()
    private[this] val soundTableView  = SoundTableView()

    if (queryInit .nonEmpty) searchView    .query  = queryInit
    if (filterInit.nonEmpty) searchView    .filter = filterInit
    if (soundInit .nonEmpty) soundTableView.sounds = soundInit

    lazy val component: Component = {
      val tabs = new TabbedPane
      tabs.pages += new TabbedPane.Page("Search" , searchView    .component)
      tabs.pages += new TabbedPane.Page("Results", soundTableView.component)
      tabs
    }
  }
}
