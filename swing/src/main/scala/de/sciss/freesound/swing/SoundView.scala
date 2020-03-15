/*
 *  SoundView.scala
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

import java.text.SimpleDateFormat
import java.util.Date

import de.sciss.freesound.Sound

import scala.swing.Component
import impl.{SoundViewImpl => Impl}
import org.json4s.DefaultFormats

object SoundView {
  def apply(): SoundView = Impl()

  def fileSizeString(bytes: Long): String = {
    val si    = true
    val unit  = if (si) 1000 else 1024
    if (bytes < unit) s"$bytes B"
    else {
      val exp = (math.log(bytes) / math.log(unit)).toInt
      val pre = s"${(if (si) "kMGTPE" else "KMGTPE").charAt(exp - 1)}${if (si) "" else "i"}"
      String.format("%.1f %sB", (bytes / math.pow(unit, exp)).asInstanceOf[java.lang.Double], pre)
    }
  }

  private[this] val df = {
    val res = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
    res.setTimeZone(DefaultFormats.UTC)
    res
  }

  def createdString(d: Date): String = df.format(d)
}
trait SoundView {
  def component: Component

  var sound: Option[Sound]
}