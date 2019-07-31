/*
 *  Filter.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound

import de.sciss.freesound.Filter.StringTokens
import de.sciss.optional.Optional
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

object Filter {
  private final implicit class OptionalBooleanOps(private val opt: Optional[Boolean]) /* extends AnyVal */ {
    def mkParam(key: String): Option[String] = opt.map(value => s"$key:$value")
  }

  private final implicit class QueryExprOps(private val opt: QueryExpr.Option) extends AnyVal {
    def mkParam(key: String): Option[String] = opt.toQueryOption.map(_.toQueryString(key))
  }

  private final implicit class GeoTagExprOps(private val opt: GeoTag.Expr) extends AnyVal {
    def mkParam: Option[String] = opt.toOption.map(_.toQueryString)
  }

  // XXX TODO --- how are tokenized strings different?
  type StringTokens = StringExpr.Option

  implicit object serializer extends ImmutableSerializer[Filter] {
    private[this] val COOKIE = 0x46534669  // "FSFi" - freesound filter

    private[this] val BooleanS = ImmutableSerializer.option[Boolean]

    def read(in: DataInput): Filter = {
      val c = in.readInt()
      require(c == COOKIE, s"Unexpected cookie (found ${c.toHexString}, expected ${COOKIE.toHexString})")
      val StringS   = StringExpr  .Option.serializer
      val UIntS     = UIntExpr    .Option.serializer
      val UDoubleS  = UDoubleExpr .Option.serializer
      import FileTypeExpr.Option.{serializer => FileTypeS}
      import DateExpr    .Option.{serializer => DateS}
      import LicenseExpr .Option.{serializer => LicenseS}
      import GeoTag.Expr        .{serializer => GeoTagS}
      val id            = UIntS     .read(in)
      val fileName      = StringS   .read(in)
      val tags          = StringS   .read(in)
      val description   = StringS   .read(in)
      val userName      = StringS   .read(in)
      val created       = DateS     .read(in)
      val license       = LicenseS  .read(in)
      val pack          = StringS   .read(in)
      val packTokens    = StringS   .read(in)
//      val geoTag        = BooleanS  .read(in)
      val geoTag        = GeoTagS   .read(in)
      val fileType      = FileTypeS .read(in)
      val duration      = UDoubleS  .read(in)
      val numChannels   = UIntS     .read(in)
      val sampleRate    = UIntS     .read(in)
      val bitDepth      = UIntS     .read(in)
      val bitRate       = UIntS     .read(in)
      val fileSize      = UIntS     .read(in)
      val numDownloads  = UIntS     .read(in)
      val avgRating     = UDoubleS  .read(in)
      val numRatings    = UIntS     .read(in)
      val comment       = StringS   .read(in)
      val numComments   = UIntS     .read(in)
      val isRemix       = BooleanS  .read(in)
      val wasRemixed    = BooleanS  .read(in)
      val md5           = StringS   .read(in)

      Filter(id = id, fileName = fileName, tags = tags, description = description, userName = userName,
        created = created, license = license, pack = pack, packTokens = packTokens, geoTag = geoTag,
        fileType = fileType, duration = duration, numChannels = numChannels, sampleRate = sampleRate,
        bitDepth = bitDepth, bitRate = bitRate, fileSize = fileSize, numDownloads = numDownloads,
        avgRating = avgRating, numRatings = numRatings, comment = comment, numComments = numComments,
        isRemix = isRemix, wasRemixed = wasRemixed, md5 = md5)
    }

    def write(v: Filter, out: DataOutput): Unit = {
      out.writeInt(COOKIE)
      import v._
      val StringS   = StringExpr  .Option.serializer
      val UIntS     = UIntExpr    .Option.serializer
      val UDoubleS  = UDoubleExpr .Option.serializer
      import FileTypeExpr.Option.{serializer => FileTypeS}
      import DateExpr    .Option.{serializer => DateS}
      import LicenseExpr .Option.{serializer => LicenseS}
      import GeoTag.Expr        .{serializer => GeoTagS}
      UIntS     .write(id           , out)
      StringS   .write(fileName     , out)
      StringS   .write(tags         , out)
      StringS   .write(description  , out)
      StringS   .write(userName     , out)
      DateS     .write(created      , out)
      LicenseS  .write(license      , out)
      StringS   .write(pack         , out)
      StringS   .write(packTokens   , out)
      GeoTagS   .write(geoTag       , out)
      FileTypeS .write(fileType     , out)
      UDoubleS  .write(duration     , out)
      UIntS     .write(numChannels  , out)
      UIntS     .write(sampleRate   , out)
      UIntS     .write(bitDepth     , out)
      UIntS     .write(bitRate      , out)
      UIntS     .write(fileSize     , out)
      UIntS     .write(numDownloads , out)
      UDoubleS  .write(avgRating    , out)
      UIntS     .write(numRatings   , out)
      StringS   .write(comment      , out)
      UIntS     .write(numComments  , out)
      BooleanS  .write(isRemix      , out)
      BooleanS  .write(wasRemixed   , out)
      StringS   .write(md5          , out)
    }
  }
}

/** The definition of a search filter. By default, all fields
  * are in their undefined state, so one can use named arguments
  * to add filter criteria.
  *
  * @param id                 sound id on freesound
  * @param fileName           string, tokenized
  * @param tags               string
  * @param description        string, tokenized
  * @param userName           string, not tokenized
  * @param created            date
  * @param license            license restriction
  * @param pack               string
  * @param packTokens         string, tokenized
  * @param geoTag             boolean
  * @param fileType           string, original file type (“wav”, “aif”, “aiff”, “ogg”, “mp3” or “flac”)
  * @param duration           numerical, duration of sound in seconds
  * @param numChannels        integer, number of channels in sound (mostly 1 or 2)
  * @param sampleRate         integer
  * @param bitDepth           integer, WARNING is not to be trusted right now
  * @param bitRate            numerical, WARNING is not to be trusted right now
  * @param fileSize           integer, file size in bytes
  * @param numDownloads       integer
  * @param avgRating          numerical, average rating, from 0 to 5
  * @param numRatings         integer, number of ratings
  * @param comment            string, tokenized (filter is satisfied if sound contains the specified value in at least one of its comments)
  * @param numComments        numerical, number of comments
  * @param isRemix            boolean
  * @param wasRemixed         boolean
  * @param md5                string, 32-byte md5 hash of file
  */
final case class Filter(
    id          : UIntExpr    .Option = None,
    fileName    : StringTokens        = None,
    tags        : StringExpr  .Option = None,
    description : StringTokens        = None,
    userName    : StringExpr  .Option = None,
    created     : DateExpr    .Option = None,
    license     : LicenseExpr .Option = None,
    pack        : StringExpr  .Option = None,
    packTokens  : StringTokens        = None,
    geoTag      : GeoTag.Expr         = GeoTag.Ignore,
    fileType    : FileTypeExpr.Option = None,
    duration    : UDoubleExpr .Option = None,
    numChannels : UIntExpr    .Option = None,
    sampleRate  : UIntExpr    .Option = None,
    bitDepth    : UIntExpr    .Option = None,
    bitRate     : UIntExpr    .Option = None,
    fileSize    : UIntExpr    .Option = None,
    numDownloads: UIntExpr    .Option = None,
    avgRating   : UDoubleExpr .Option = None,
    numRatings  : UIntExpr    .Option = None,
    comment     : StringTokens        = None,
    numComments : UIntExpr    .Option = None,
    isRemix     : Optional[Boolean]   = None,
    wasRemixed  : Optional[Boolean]   = None,
    md5         : StringExpr  .Option = None
  ) {

  override def toString: String = s"$productPrefix(${toPropertyOption.getOrElse("")})"

  def isDefined: Boolean = toPropertyOption.isDefined
  def isEmpty  : Boolean = toPropertyOption.isEmpty
  def nonEmpty : Boolean = isDefined

//  require(avgRating.startOption.forall(_ <= 5) &&
//    avgRating.endOption.forall(_ <= 5),
//    s"avgRating out of range: $avgRating")

  def toPropertyOption: Option[String] = {
    import Filter.{OptionalBooleanOps, QueryExprOps, GeoTagExprOps}
    val options = Seq(
      id            .mkParam("id"),
      fileName      .mkParam("original_filename"),
      tags          .mkParam("tag"),
      description   .mkParam("description"),
      userName      .mkParam("username"),
      created       .mkParam("created"),
      license       .mkParam("license"),
      pack          .mkParam("pack"),
      packTokens    .mkParam("pack_tokenized"),
      geoTag        .mkParam,
      fileType      .mkParam("type"),
      duration      .mkParam("duration"),
      numChannels   .mkParam("channels"),
      sampleRate    .mkParam("samplerate"),
      bitDepth      .mkParam("bitdepth"),
      bitRate       .mkParam("bitrate"),
      fileSize      .mkParam("filesize"),
      numDownloads  .mkParam("num_downloads"),
      avgRating     .mkParam("avg_rating"),
      numRatings    .mkParam("num_ratings"),
      comment       .mkParam("comment"),
      numComments   .mkParam("comments"),
      isRemix       .mkParam("is_remix"),
      wasRemixed    .mkParam("was_remixed"),
      md5           .mkParam("md5")
    )
    val params = options.flatten
    if (params.isEmpty) None else Some(params.mkString(" "))
  }
}