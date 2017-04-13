/*
 *  Previews.scala
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

import java.net.URI

/** Links to preview renderings of a sound in two different qualities
  * and sound file formats.
  */
final case class Previews(mp3Low: URI, mp3High: URI, oggLow: URI, oggHigh: URI) {
  override def toString = s"$productPrefix(mp3Low = $mp3Low, mp3High = $mp3High, ...)"
}