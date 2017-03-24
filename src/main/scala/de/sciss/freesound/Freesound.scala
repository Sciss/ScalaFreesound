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

import java.text.SimpleDateFormat
import java.util.Locale

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object Freesound {
  var verbose         = true
  var tmpPath: String = System.getProperty("java.io.tmpdir")

  var loginURL        = "http://www.freesound.org/forum/login.php"
  var searchURL       = "http://www.freesound.org/searchTextXML.php"
  var infoURL         = "http://www.freesound.org/samplesViewSingleXML.php"
  var downloadURL     = "http://www.freesound.org/samplesDownload.php"

  val dateFormat      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  def login(userName: String, password: String): LoginProcess =
    ??? // new LoginProcessImpl(userName, password)

  def apply(token: String): Freesound = impl.FreesoundImpl(token)
}
trait Freesound {
//  def search(options: SearchOptions): Future[Vec[Sample]]
  def textSearch(options: TextSearch): Future[Vec[Sample]]
}