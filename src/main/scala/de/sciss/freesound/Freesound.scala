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

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object Freesound {
//  var verbose         = true
//  var tmpPath: String = System.getProperty("java.io.tmpdir")

//  val dateFormat      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  val urlApiBase        = "https://www.freesound.org/apiv2"
  /** URL for starting a text search. */
  var urlTextSearch     = s"$urlApiBase/search/text/"
  /** URL for downloading a sound.
    * A placeholder `%s` will be replaced by the sound id.
    */
  var urlSoundDownload  = s"$urlApiBase/sounds/%s/download/"

  def apply(apiKey: String): Freesound = impl.FreesoundImpl(apiKey)
}
trait Freesound {
  def run(options: TextSearch): Future[Vec[Sound]]
}