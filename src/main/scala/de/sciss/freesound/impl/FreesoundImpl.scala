/*
 *  FreesoundImpl.scala
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
package impl

import java.net.URI

import com.ning.http.client.Response
import org.json4s.{DefaultFormats, Formats, Serializer, StringInput, TypeInfo}
import org.json4s.JsonAST.{JString, JValue}
import org.json4s.native.JsonMethods.parse

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object FreesoundImpl {
  def apply(apiKey: String): Freesound = new Impl(apiKey)

  private final case class ResultPage(count: Int, next: String, results: Vector[Sound], previous: String)

  private trait Deserializer[A] extends Serializer[A] {
    final def serialize(implicit format: Formats): PartialFunction[Any, JValue] =
      throw new UnsupportedOperationException
  }

  private final object URISerializer extends Deserializer[URI] {
    private[this] val Clazz = classOf[URI]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), URI] = {
      case (TypeInfo(Clazz, _), JString(s)) => new URI(s)
    }
  }

  private final object FileTypeSerializer extends Deserializer[FileType] {
    private[this] val Clazz = classOf[FileType]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), FileType] = {
      case (TypeInfo(Clazz, _), JString(s)) => FileType.fromString(s)
    }
  }

  private final object GeoTagSerializer extends Deserializer[GeoTag] {
    private[this] val Clazz = classOf[GeoTag]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), GeoTag] = {
      case (TypeInfo(Clazz, _), JString(s)) =>
        val Array(latS, lonS) = s.split(' ')
        GeoTag(lat = latS.toDouble, lon = lonS.toDouble)
    }
  }

  private final implicit val jsonFormats: Formats =
    DefaultFormats ++ (URISerializer :: FileTypeSerializer :: GeoTagSerializer :: Nil)

  // default `json4.Json` uses `as.String` which in turn takes charset
  // from content-type, and if not provided (which I guess is the case?),
  // falls back to ISO-8859-1 - which is wrong. Enforce UTF-8 here:
  private object JsonUTF extends (Response => JValue) {
    def apply(r: Response): JValue =
      (dispatch.as.String.utf8 andThen (s => parse(StringInput(s), useBigDecimalForDouble = true)))(r)
  }

  private final class Impl(apiKey: String) extends Freesound {
    override def toString = s"Freesound@${hashCode.toHexString}"

    def run(options: TextSearch): Future[Vec[Sound]] = {
      import dispatch._, Defaults._
      var params  = options.toFields.iterator.map { case QueryField(key, value) => (key, value) } .toMap
      params += "token" -> apiKey
      params += "fields" -> "id,name,tags,description,username,created,license,pack,geotag,type,duration,channels,samplerate,bitdepth,bitrate,filesize,num_downloads,avg_rating,num_ratings,num_comments"

      val req0    = url(Freesound.urlTextSearch)
      val req     = req0 <<? params
//      val req     = req1.setContentType("application/json", "UTF-8")
//      println(req.url)
      val futJson = Http(req.OK(JsonUTF))
      futJson.map { json =>
          val mapped = json.mapField {
          case (k @ "results", v0) => k -> v0.mapField {
            case ("name"          , v) => ("fileName"     , v)
            case ("username"      , v) => ("userName"     , v)
            case ("geotag"        , v) => ("geoTag"       , v)
            case ("type"          , v) => ("fileType"     , v)
            case ("channels"      , v) => ("numChannels"  , v)
            case ("samplerate"    , v) => ("sampleRate"   , v)
            case ("bitdepth"      , v) => ("bitDepth"     , v)
            case ("bitrate"       , v) => ("bitRate"      , v)
            case ("filesize"      , v) => ("fileSize"     , v)
            case ("num_downloads" , v) => ("numDownloads" , v)
            case ("avg_rating"    , v) => ("avgRating"    , v)
            case ("num_ratings"   , v) => ("numRatings"   , v)
            case ("num_comments"  , v) => ("numComments"  , v)
            case other => other
          }
          case other => other
        }
        val res = mapped.extract[ResultPage]
        res.results // .toVector
      }
    }
  }
}
