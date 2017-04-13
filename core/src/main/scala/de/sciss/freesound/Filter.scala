/*
 *  Filter.scala
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

import de.sciss.freesound.Filter.StringTokens
import de.sciss.optional.Optional

import scala.language.implicitConversions

object Filter {
  private final implicit class OptionalBooleanOps(private val opt: Optional[Boolean]) /* extends AnyVal */ {
    def mkParam(key: String): Option[String] = opt.map(value => s"$key:$value")
  }

  private final implicit class QueryExprOps(private val opt: QueryExpr.Option) extends AnyVal {
    def mkParam(key: String): Option[String] = opt.toQueryOption.map(expr => expr.toQueryString(key))
  }

  // XXX TODO --- how are tokenized strings different?
  type StringTokens = StringExpr.Option
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
  * @param license            string (“Attribution”, “Attribution Noncommercial” or “Creative Commons 0”)
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
    license     : StringExpr  .Option = None,
    pack        : StringExpr  .Option = None,
    packTokens  : StringTokens        = None,
    geoTag      : Optional[Boolean]   = None,
    fileType    : FileTypeExpr.Option = None,
    duration    : UDoubleExpr .Option = None,
    numChannels : UIntExpr    .Option = None,
    sampleRate  : UIntExpr    .Option = None,
    bitDepth    : UIntExpr    .Option = None,
//    bitRate     : UDoubleExpr .Option = None,
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
    import Filter.{OptionalBooleanOps, QueryExprOps}
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
      geoTag        .mkParam("is_geotagged"),
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