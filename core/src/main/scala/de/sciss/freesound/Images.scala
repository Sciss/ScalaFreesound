/*
 *  Images.scala
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

/** Links to visual renderings of a sound in two different qualities
  * and domains (time and spectral).
  *
  * @param  waveLow       URI of png waveform image at lower resolution (120 x 71 pixels)
  * @param  waveHigh      URI of png waveform image at higher resolution (900 x 201 pixels)
  * @param  spectralLow   URI of png spectrogram image at lower resolution (120 x 71 pixels)
  * @param  spectralHigh  URI of png spectrogram image at higher resolution (900 x 201 pixels)
  */
final case class Images(waveLow: URI, waveHigh: URI, spectralLow: URI, spectralHigh: URI) {
  override def toString = s"$productPrefix(waveHigh = $waveHigh, spectralHigh = $spectralHigh , ...)"

  /** Programmatically determines one of the four URIs. */
  def uri(spectral: Boolean, hq: Boolean): URI =
    if (spectral) { if (hq) spectralHigh else spectralLow }
    else          { if (hq) waveHigh     else waveLow     }
}