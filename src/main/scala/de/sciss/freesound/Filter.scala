package de.sciss.freesound

import de.sciss.freesound.Filter.StringTokens
import de.sciss.optional.Optional

import scala.language.implicitConversions

object Filter {
  private final implicit class OptionalBooleanOps(private val opt: Optional[Boolean]) /* extends AnyVal */ {
    def mkParam(key: String): Option[String] = opt.map(value => s"$key=$value")
  }

  type StringTokens = StringExpr.Option
}

/** The definition of a search filter. By default, all fields
  * are in their undefined state, so one can use named arguments
  * to add filter criteria.
  *
  * @param id                 sound id on freesound
  * @param userName           string, not tokenized
  * @param created            date
  * @param fileName           string, tokenized
  * @param description        string, tokenized
  * @param tag                string
  * @param license            string (“Attribution”, “Attribution Noncommercial” or “Creative Commons 0”)
  * @param isRemix            boolean
  * @param wasRemixed         boolean
  * @param pack               string
  * @param packTokens         string, tokenized
  * @param geoTagged          boolean
  * @param fileType           string, original file type (“wav”, “aif”, “aiff”, “ogg”, “mp3” or “flac”)
  * @param duration           numerical, duration of sound in seconds
  * @param bitDepth           integer, WARNING is not to be trusted right now
  * @param bitRate            numerical, WARNING is not to be trusted right now
  * @param sampleRate         integer
  * @param fileSize           integer, file size in bytes
  * @param numChannels        integer, number of channels in sound (mostly 1 or 2)
  * @param md5                string, 32-byte md5 hash of file
  * @param numDownloads       integer
  * @param avgRating          numerical, average rating, from 0 to 5
  * @param numRatings         integer, number of ratings
  * @param comment            string, tokenized (filter is satisfied if sound contains the specified value in at least one of its comments)
  * @param numComments        numerical, number of comments
  */
final case class Filter(
    id          : UIntExpr    .Option = None,
    userName    : StringExpr  .Option = None,
    created     : DateExpr    .Option = None,
    fileName    : StringTokens        = None,
    description : StringTokens        = None,
    tag         : StringExpr  .Option = None,
    license     : StringExpr  .Option = None,
    isRemix     : Optional[Boolean]   = None,
    wasRemixed  : Optional[Boolean]   = None,
    pack        : StringExpr  .Option = None,
    packTokens  : StringTokens        = None,
    geoTagged   : Optional[Boolean]   = None,
    fileType    : FileTypeExpr.Option = None,
    duration    : UDoubleExpr .Option = None,
    bitDepth    : UIntExpr    .Option = None,
    bitRate     : UDoubleExpr .Option = None,
    sampleRate  : UIntExpr    .Option = None,
    fileSize    : UIntExpr    .Option = None,
    numChannels : UIntExpr    .Option = None,
    md5         : StringExpr  .Option = None,
    numDownloads: UIntExpr    .Option = None,
    avgRating   : UDoubleExpr .Option = None,
    numRatings  : UIntExpr    .Option = None,
    comment     : StringTokens        = None,
    numComments : UIntExpr    .Option = None
  ) {

//  require(avgRating.startOption.forall(_ <= 5) &&
//    avgRating.endOption.forall(_ <= 5),
//    s"avgRating out of range: $avgRating")

  def toPropertyOption: Option[String] = {
    ???
//    import FilterNew.OptionalBooleanOps
//    val options = Seq(
//      id            .mkParam("id"),
//      userName      .mkParam("username"),
//      created       .mkParam("created"),
//      fileName      .mkParam("original_filename"),
//      description   .mkParam("description"),
//      tag           .mkParam("tag"),
//      license       .mkParam("license"),
//      isRemix       .mkParam("is_remix"),
//      wasRemixed    .mkParam("was_remixed"),
//      pack          .mkParam("pack"),
//      packTokens    .mkParam("pack_tokenized"),
//      geoTagged     .mkParam("is_geotagged"),
//      fileType      .mkParam("type"),
//      duration      .mkParam("duration"),
//      bitDepth      .mkParam("bitdepth"),
//      bitRate       .mkParam("bitrate"),
//      sampleRate    .mkParam("samplerate"),
//      fileSize      .mkParam("filesize"),
//      numChannels   .mkParam("channels"),
//      md5           .mkParam("md5"),
//      numDownloads  .mkParam("num_downloads"),
//      avgRating     .mkParam("avg_rating"),
//      numRatings    .mkParam("num_ratings"),
//      comment       .mkParam("comment"),
//      numComments   .mkParam("comments")
//    )
//    val params = options.flatten
//    if (params.isEmpty) None else Some(params.mkString(" "))
  }
}