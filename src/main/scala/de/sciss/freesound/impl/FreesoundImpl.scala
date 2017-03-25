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

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object FreesoundImpl {
  def apply(token: String): Freesound = new Impl(token)

  private final case class ResultPage(count: Int, next: String, results: Vector[Sound], previous: String)

  private final class Impl(token: String) extends Freesound {
    override def toString = s"Freesound@${hashCode.toHexString}"

    def run(options: TextSearch): Future[Vec[Sound]] = {
      import dispatch._, Defaults._
      var params  = options.toFields.iterator.map { case QueryField(key, value) => (key, value) } .toMap
      params += "token" -> token
      params += "fields" -> "id,name,tags,description,username,created,license,pack,geotag,type,duration,channels,samplerate,bitdepth,bitrate,filesize,num_downloads,avg_rating,num_ratings,num_comments"

      val req0    = host("www.freesound.org") / "apiv2" / "search" / "text" / ""
      val req     = req0 <<? params
      println(req.url)
      val futJson = Http(req.OK(as.json4s.Json))
      futJson.map { json =>
        implicit val fmt: DefaultFormats = DefaultFormats
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
