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
  *
  * Note: SuperCollider can play back OGG Vorbis files, but not mp3.
  * The lower resolution files take roughly half of the size of the
  * higher resolution ones, and for previewing purposes the quality if sufficient.
  *
  * @param  oggLow  URI for a lower resolution (80 kb/s) Ogg Vorbis preview
  * @param  oggHigh URI for a higher resolution (192 kb/s) Ogg Vorbis preview
  * @param  mp3Low  URI for a lower resolution (64 kb/s) mp3 preview
  * @param  mp3High URI for a higher resolution (128 kb/s) mp3 preview
  */
final case class Previews(oggLow: URI, oggHigh: URI, mp3Low: URI, mp3High: URI) {
  override def toString = s"$productPrefix(oggLow = $oggLow, oggHigh = $oggHigh , ...)"

  /** Programmatically determines one of the four URIs. */
  def uri(ogg: Boolean, hq: Boolean): URI =
    if (ogg) { if (hq) oggHigh else oggLow }
    else     { if (hq) mp3High else mp3Low }
}