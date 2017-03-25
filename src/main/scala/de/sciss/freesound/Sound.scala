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
//    created     : Date,
    license     : String, // URI
    pack 	      : Option[String], // Option[URI],
    geoTag      : Option[GeoTag],
//    fileType    : FileType,
    duration    : Double,
    numChannels : Int,
    sampleRate  : Double,
    bitDepth    : Int,
    bitRate     : Double,
    fileSize    : Long,
    numDownloads: Int,
    avgRating   : Double,
    numRatings  : Int,
    numComments : Int
  )


//- images 	object 	Dictionary including the URIs for spectrogram and waveform visualizations of the sound. The dinctionary includes the fields waveform_l and waveform_m (for large and medium waveform images respectively), and spectral_l and spectral_m (for large and medium spectrogram images respectively).
//- analysis 	object 	Object containing requested descriptors information according to the descriptors request parameter (see below). This field will be null if no descriptors were specified (or invalid descriptor names specified) or if the analysis data for the sound is not available.
