/*
 *  Sound.scala
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
import java.util.Date

import scala.util.Try

/** Database record of a sound.
  *
  * @param id           the unique identifier on the Freesound platform
  * @param fileName     the name given to the sound by the uploader
  * @param tags         list of tags describing the sound
  * @param description  verbose description of the sound
  * @param userName     Freesound user-name of the uploader
  * @param license      URI of the license applying to the usage of the sound
  * @param pack         optional URI to the collection that contains the sound
  * @param geoTag       optional geographical location where the sound was recorded
  * @param duration     duration in seconds
  * @param numChannels  number of channels
  * @param sampleRate   sample rate in Hertz
  * @param bitDepth     bit depth (number of bits per sample frame)
  * @param bitRate      bit rate in kbps
  * @param fileSize     file size in bytes
  * @param numDownloads number of times the sound has been downloaded
  * @param avgRating    average rating of the second (0 to 5)
  * @param numRatings   number of times the sound has been rated
  * @param numComments  number of comments made by users on the sound
  */
final case class Sound(
    id          : Int,
    fileName    : String,
    tags        : List[String],
    description : String,
    userName    : String,
    created     : String, // Date,
    license     : License,
    pack        : Option[URI],
    geoTag      : Option[GeoTag],
    fileType    : FileType,
    duration    : Double,
    numChannels : Int,
    sampleRate  : Double,
    bitDepth    : Option[Int],
    bitRate     : Int, // Double,
    fileSize    : Long,
    numDownloads: Int,
    avgRating   : Double,
    numRatings  : Int,
    numComments : Int,
    previews    : Option[Previews]
  ) {

  def packId: Option[Int] = pack.flatMap { uri =>
    val xs = uri.getPath
    val i  = xs.lastIndexOf('/')
    val j  = xs.lastIndexOf('/', i - 1) + 1
    if (j >= i) None else {
      val s = xs.substring(j, i)
      Try(s.toInt).toOption
    }
  }

  override def toString: String =
    f"""Sound($id,
       |  fileName    = $fileName,
       |  tags        = $tags,
       |  description = $description,
       |  userName    = $userName,
       |  created     = $created,
       |  license     = $license,
       |  pack 	      = $pack,
       |  geoTag      = $geoTag,
       |  fileType    = $fileType,
       |  duration    = $duration%1.3f,
       |  numChannels = $numChannels,
       |  sampleRate  = $sampleRate%1.1f,
       |  bitDepth    = ${bitDepth.getOrElse(0)},
       |  bitRate     = $bitRate,
       |  fileSize    = $fileSize,
       |  numDownloads= $numDownloads,
       |  avgRating   = $avgRating%1.1f,
       |  numRatings  = $numRatings,
       |  numComments = $numComments,
       |  previews    = $previews
       |)""".stripMargin

  /** Constructs a new file name based on the `id` and `fileType` of this sound.
    * E.g. if `id` is `1234` and `fileType` is `FileType.AIFF`, the methods
    * returns `1234.aif`.
    */
  def uniqueFileName: String = s"$id.${fileType.toProperty}"
}

//- images 	object 	Dictionary including the URIs for spectrogram and waveform visualizations of the sound. The dictionary includes the fields waveform_l and waveform_m (for large and medium waveform images respectively), and spectral_l and spectral_m (for large and medium spectrogram images respectively).
//- analysis 	object 	Object containing requested descriptors information according to the descriptors request parameter (see below). This field will be null if no descriptors were specified (or invalid descriptor names specified) or if the analysis data for the sound is not available.
