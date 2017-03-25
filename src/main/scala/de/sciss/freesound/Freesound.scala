/*
 *  Freesound.scala
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

import java.io.File

import de.sciss.processor.Processor
import de.sciss.freesound.impl.{FreesoundImpl => Impl}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object Freesound {
//  val dateFormat      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  val urlApiBase        = "https://www.freesound.org/apiv2"
  /** URL for starting a text search. */
  var urlTextSearch     = s"$urlApiBase/search/text/"
  /** URL for downloading a sound.
    * A placeholder `%s` will be replaced by the sound id.
    */
  var urlSoundDownload  = s"$urlApiBase/sounds/%s/download/"

  def textSearch(query: String, filter: Filter = Filter(), sort: Sort = Sort.Score,
          groupByPack: Boolean = false, maxItems: Int = 100)(implicit api: ApiKey): Future[Vec[Sound]] =
    Impl.textSearch(query = query, filter = filter, sort = sort, groupByPack = groupByPack, maxItems = maxItems)

  def download(id: Int, out: File)(implicit access: AccessToken): Processor[Unit] =
    Impl.download(id = id, out = out)
}