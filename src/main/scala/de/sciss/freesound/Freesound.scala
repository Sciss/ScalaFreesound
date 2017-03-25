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

import scala.concurrent.Future

object Freesound {
  var verbose         = true
  var tmpPath: String = System.getProperty("java.io.tmpdir")

  val dateFormat      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  def apply(token: String): Freesound = impl.FreesoundImpl(token)
}
trait Freesound {
  def run(options: TextSearch): Future[String] // [Vec[Sample]]
}