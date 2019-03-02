/*
 *  Sound.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

object Sound {
  implicit object serializer extends ImmutableSerializer[Sound] {
    private[this] val COOKIE = 0x4653536e // "FSSn"

    def read(in: DataInput): Sound = {
      import in._
      val c = readInt()
      require(c == COOKIE, s"Unexpected cookie (found ${c.toHexString}, expected ${COOKIE.toHexString})")
      val id            = readInt   ()
      val fileName      = readUTF   ()
      val tagsSz        = readShort ()
      val tags          = List.fill(tagsSz)(readUTF())
      val description   = readUTF   ()
      val userName      = readUTF   ()
      val created       = new Date(readLong())
      val license       = License.serializer.read(in)
      val packId        = readInt   ()
      val geoTag        = if (readByte() == 0) None else {
        val lat = readDouble()
        val lon = readDouble()
        Some(GeoTag(lat = lat, lon = lon))
      }
      val fileType      = FileType.serializer.read(in)
      val duration      = readDouble()
      val numChannels   = readInt   ()
      val sampleRate    = readDouble()
      val bitDepth      = readInt   ()
      val bitRate       = readInt   ()
      val fileSize      = readLong  ()
      val numDownloads  = readInt   ()
      val avgRating     = readDouble()
      val numRatings    = readInt   ()
      val numComments   = readInt   ()
      val userId        = readInt   ()
      Sound(id = id, fileName = fileName, tags = tags, description = description, userName = userName,
        created = created, license = license, packId = packId, geoTag = geoTag, fileType = fileType,
        duration = duration, numChannels = numChannels, sampleRate = sampleRate, bitDepth = bitDepth,
        bitRate = bitRate, fileSize = fileSize, numDownloads = numDownloads, avgRating = avgRating,
        numRatings = numRatings, numComments = numComments, userId = userId)
    }

    def write(v: Sound, out: DataOutput): Unit = {
      import out._
      import v._
      writeInt(COOKIE)
      writeInt   (id         )
      writeUTF   (fileName   )
      writeShort (tags.size)
      tags.foreach(writeUTF)
      writeUTF   (description)
      writeUTF   (userName   )
      writeLong  (created.getTime)
      License.serializer.write(license, out)
      writeInt   (packId     )
      geoTag.fold(writeByte(0)) { gt =>
        writeByte(1)
        writeDouble(gt.lat)
        writeDouble(gt.lon)
      }
      FileType.serializer.write(fileType, out)
      writeDouble(duration    )
      writeInt   (numChannels )
      writeDouble(sampleRate  )
      writeInt   (bitDepth    )
      writeInt   (bitRate     )
      writeLong  (fileSize    )
      writeInt   (numDownloads)
      writeDouble(avgRating   )
      writeInt   (numRatings  )
      writeInt   (numComments )
      writeInt   (userId      )
    }
  }
}

/** Database record of a sound.
  *
  * @param id           the unique identifier on the Freesound platform
  * @param fileName     the name given to the sound by the uploader
  * @param tags         list of tags describing the sound
  * @param description  verbose description of the sound
  * @param userName     Freesound user-name of the uploader
  * @param license      URI of the license applying to the usage of the sound
  * @param packId       id of the collection that contains the sound,
  *                     or zero if no pack was used.
  * @param geoTag       optional geographical location where the sound was recorded
  * @param duration     duration in seconds
  * @param numChannels  number of channels
  * @param sampleRate   sample rate in Hertz
  * @param bitDepth     bit depth (number of bits per sample frame); note that
  *                     the server reports zero for some sounds
  * @param bitRate      bit rate in kbps
  * @param fileSize     file size in bytes
  * @param numDownloads number of times the sound has been downloaded
  * @param avgRating    average rating of the second (0 to 5)
  * @param numRatings   number of times the sound has been rated
  * @param userId       the unique identifier on the user that uploaded the sound
  *                     (required for preview and image URIs)
  */
final case class Sound(
    id          : Int,
    fileName    : String,
    tags        : List[String],
    description : String,
    userName    : String,
    created     : Date,
    license     : License,
    packId      : Int = 0,
    geoTag      : Option[GeoTag],
    fileType    : FileType,
    duration    : Double,
    numChannels : Int,
    sampleRate  : Double,
    bitDepth    : Int = 0,
    bitRate     : Int,
    fileSize    : Long,
    numDownloads: Int,
    avgRating   : Double,
    numRatings  : Int,
    numComments : Int,
    userId      : Int
  ) {

//  def packId: Option[Int] = pack.flatMap { uri =>
//    val xs = uri.getPath
//    val i  = xs.lastIndexOf('/')
//    val j  = xs.lastIndexOf('/', i - 1) + 1
//    if (j >= i) None else {
//      val s = xs.substring(j, i)
//      Try(s.toInt).toOption
//    }
//  }

  override def toString: String =
    f"""Sound($id,
       |  fileName    = $fileName,
       |  tags        = $tags,
       |  description = $description,
       |  userName    = $userName,
       |  created     = $created,
       |  license     = $license,
       |  packId      = $packId,
       |  geoTag      = $geoTag,
       |  fileType    = $fileType,
       |  duration    = $duration%1.3f,
       |  numChannels = $numChannels,
       |  sampleRate  = $sampleRate%1.1f,
       |  bitDepth    = $bitDepth,
       |  bitRate     = $bitRate,
       |  fileSize    = $fileSize,
       |  numDownloads= $numDownloads,
       |  avgRating   = $avgRating%1.1f,
       |  numRatings  = $numRatings,
       |  numComments = $numComments,
       |  userId      = $userId
       |)""".stripMargin

  /** Constructs a new file name based on the `id` and `fileType` of this sound.
    * E.g. if `id` is `1234` and `fileType` is `FileType.AIFF`, the methods
    * returns `1234.aif`.
    */
  def uniqueFileName: String = s"$id.${fileType.toProperty}"

  def previewUri(ogg: Boolean, hq: Boolean): URI = {
    val s = Freesound.urlSoundPreview.format(id / 1000, id, userId, if (hq) "hq" else "lq", if (ogg) "ogg" else "mp3")
    new URI(s)
  }

  def imageUri(spectral: Boolean, hq: Boolean): URI = {
    val suf = if (spectral) { if (hq) "spec_L.jpg" else "spec_M.jpg" }
              else          { if (hq) "wave_L.png" else "wave_M.png" }
    val s = Freesound.urlImage.format(id / 1000, id, userId, if (hq) "hq" else "lq", suf)
    new URI(s)
  }
}

//- analysis 	object 	Object containing requested descriptors information according to the descriptors request parameter (see below). This field will be null if no descriptors were specified (or invalid descriptor names specified) or if the analysis data for the sound is not available.
