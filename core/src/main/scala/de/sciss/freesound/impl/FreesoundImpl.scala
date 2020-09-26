/*
 *  FreesoundImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.impl

import java.io.FileOutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar

import de.sciss.file._
import de.sciss.freesound.{Auth, Client, FileType, Filter, Freesound, GeoTag, License, QueryField, Sort, Sound, TextSearch}
import de.sciss.processor.Processor
import dispatch.Http
import org.asynchttpclient.Response
import org.json4s.JsonAST.{JInt, JObject, JString, JValue}
import org.json4s.native.JsonMethods
import org.json4s.native.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats, Serializer, StringInput, TypeInfo}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.{Future, blocking}
import scala.util.Try

object FreesoundImpl {
  // def apply(apiKey: String): Freesound = new Impl(apiKey)

  private final case class ResultPage(count: Int, next: Option[String], results: Vector[Sound])

  private trait Deformat[A] extends Serializer[A] {
    final def serialize(implicit format: Formats): PartialFunction[Any, JValue] =
      throw new UnsupportedOperationException
  }

  private final object URIFormat extends Deformat[URI] {
    private[this] val Clazz = classOf[URI]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), URI] = {
      case (TypeInfo(Clazz, _), JString(s)) => new URI(s)
    }
  }

  private final object LicenseFormat extends Deformat[License] {
    private[this] val Clazz = classOf[License]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), License] = {
      case (TypeInfo(Clazz, _), JString(s)) => License.parse(new URI(s))
    }
  }

  private final object FileTypeFormat extends Deformat[FileType] {
    private[this] val Clazz = classOf[FileType]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), FileType] = {
      case (TypeInfo(Clazz, _), JString(s)) => FileType.fromString(s)
    }
  }

  private final object GeoTagFormat extends Deformat[GeoTag] {
    private[this] val Clazz = classOf[GeoTag]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), GeoTag] = {
      case (TypeInfo(Clazz, _), JString(GeoTag(geo))) => geo
    }
  }

  private object Fmt {
    implicit val default: Formats = DefaultFormats

    private object MyDefault extends DefaultFormats {
      override protected def dateFormatter: SimpleDateFormat = {
        // time zone is not give; seconds are given as decimal
        // number with six fractional digits which cannot be parsed (?)
        // by SimpleDateFormat, so we drop the millis.
        val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        f.setTimeZone(DefaultFormats.UTC)
        f
      }
    }

    implicit val page: Formats =
      MyDefault ++ (URIFormat :: LicenseFormat :: FileTypeFormat :: GeoTagFormat :: Nil)
  }

//  private final implicit val jsonFormats: Formats =
//    DefaultFormats ++ (URIFormat :: LicenseFormat :: FileTypeFormat :: GeoTagFormat :: Nil)

  // default `json4.Json` uses `as.String` which in turn takes charset
  // from content-type, and if not provided (which I guess is the case?),
  // falls back to ISO-8859-1 - which is wrong. Enforce UTF-8 here:
  private object JsonUTF extends (Response => JValue) {
    def apply(r: Response): JValue =
      (dispatch.as.String.utf8 andThen (s => parse(StringInput(s), useBigDecimalForDouble = true)))(r)
  }

  def readClient(f: File = file("client.json")): Client = {
    import Fmt.default
    val json = JsonMethods.parse(f)
    json.extract[Client]
  }

  def readAuth(f: File = file("auth.json")): Auth = {
    import Fmt.default
    val json0 = JsonMethods.parse(f)
    val json  = json0.camelizeKeys
    json.extract[Auth]
  }

  def writeAuth(f: File = file("auth.json"))(implicit auth: Auth): Unit = {
    import Fmt.default
    val expiresS = default.dateFormat.format(auth.expires)

    val json = JObject("access_token"  -> JString(auth.accessToken),
                       "expires"       -> JString(expiresS),
                       "refresh_token" -> JString(auth.refreshToken))
    val s = JsonMethods.pretty(JsonMethods.render(json))
    val fos = new FileOutputStream(f)
    try {
      fos.write(s.getBytes("UTF-8"))
    } finally {
      fos.close()
    }
  }

  def getAuth(code: String)(implicit client: Client)    : Future[Auth] = getToken(code             , isRefresh = false)
  def refreshAuth()(implicit client: Client, auth: Auth): Future[Auth] = getToken(auth.refreshToken, isRefresh = true )

  private def getToken(code: String, isRefresh: Boolean)(implicit client: Client): Future[Auth] = {
    import dispatch._
    import Defaults._
    import Fmt.default

    val req0    = url(Freesound.urlGetAuth)
      .addParameter("client_id", client.id)
      .addParameter("client_secret", client.secret)
      .addParameter("grant_type", if (isRefresh) "refresh_token" else "authorization_code")
      .addParameter(if (isRefresh) "refresh_token" else "code", code)

    val req   = req0.POST
    val now   = Calendar.getInstance
    val futJson = runJSON(req)
    futJson.map { json0 =>
      val json1 = json0.mapField {
        case ("expires_in", JInt(expiresIn)) =>
          now.add(Calendar.SECOND, expiresIn.intValue)
          "expires" -> JString(default.dateFormat.format(now.getTime))
        case other => other
      }
      val json = json1.camelizeKeys
      json.extract[Auth]
    }
  }

  def textSearch(query: String, filter: Filter, sort: Sort, /* previews: Boolean, images: Boolean, */
                 groupByPack: Boolean, maxItems: Int)(implicit client: Client): Future[Vec[Sound]] = {
    val options = TextSearch(query = query, filter = filter, /* previews = previews, images = images, */
      sort = sort, groupByPack = groupByPack, maxItems = maxItems)
    runTextSearch(options, page = 1, done = Vector.empty)
  }

  def textCount(query: String, filter: Filter)(implicit client: Client): Future[Int] = {
    // `maxItems = 0` is not allowed by server
    val options = TextSearch(query = query, filter = filter, maxItems = 1)
    var params  = options.toFields.iterator.map { case QueryField(key, value) => (key, value) } .toMap
    params += "token"     -> client.secret
    params += "fields"    -> "id"
    params += "page_size" -> options.maxItems.toString

    import dispatch._
    import Defaults._
    val req0    = url(Freesound.urlTextSearch)
    val req     = req0 <<? params
    val futJson = runJSON(req) // Http(req.OK(JsonUTF))
    futJson.map { json =>
      val numOpt = json match {
        case JObject(entries) =>
          entries.collectFirst {
            case ("count", JInt(num)) => num.intValue
          }
        case _ => None
      }
      numOpt.getOrElse(sys.error("Could not determined 'count' parameter"))
    }
  }

  private def extractPage(json: JValue): ResultPage = {
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
        case ("pack", JString(uri)) =>
          val i  = uri.lastIndexOf('/')
          val j  = uri.lastIndexOf('/', i - 1) + 1
          val packId = if (j >= i) 0 else {
            val s = uri.substring(j, i)
            Try(s.toInt).getOrElse(0)
          }
          ("packId", JInt(packId))

//        case (k1 @ "previews" , v) => k1 -> v.mapField {
//          case ("preview-hq-mp3", v1) => ("mp3High", v1)
//          case ("preview-lq-mp3", v1) => ("mp3Low" , v1)
//          case ("preview-hq-ogg", v1) => ("oggHigh", v1)
//          case ("preview-lq-ogg", v1) => ("oggLow" , v1)
//          case other => other
//        }
//        case (k1 @ "images" , v) => k1 -> v.mapField {
//          case ("waveform_m", v1) => ("waveLow"     , v1)
//          case ("waveform_l", v1) => ("waveHigh"    , v1)
//          case ("spectral_m", v1) => ("spectralLow" , v1)
//          case ("spectral_l", v1) => ("spectralHigh", v1)
//          case other => other
//        }
        case ("previews", JObject(entries)) =>
          val opt = entries.collectFirst {
            case ("preview-lq-ogg", JString(s)) =>
              val i       = s.lastIndexOf('_') + 1
              val j       = s.indexOf('-', i)
              val userId  = s.substring(i, j).toInt
              "userId" -> JInt(userId)
          }
          opt.get

        case other => other
      }

      case other => other
    }

    import Fmt.page
    mapped.extract[ResultPage]
  }

  var DEBUG = false

  private def runJSON(req: dispatch.Req): Future[JValue] = {
    if (DEBUG) println(s"req: ${req.url}")
    // Netty connect may block even before the future is spun up.
    // Therefore add another layer of wrapping!
    // Cf. http://stackoverflow.com/questions/43391769
    import dispatch.Defaults._
    Future.successful(()).flatMap { _ =>
      val jsonFut = blocking(
        Http.default(req.OK(JsonUTF))
      )
      jsonFut // Await.result(jsonFut, Duration.Inf)
    }
  }

  private[this] final val defaultFields =
    "id,name,tags,description,username,created,license,pack,geotag,type,duration,channels,samplerate,bitdepth," +
    "bitrate,filesize,num_downloads,avg_rating,num_ratings,num_comments,previews"
//  private[this] final val fieldsWithPreview = defaultFields + ",previews"

  private def runTextSearch(options: TextSearch, page: Int, done: Vec[Sound])
                           (implicit client: Client): Future[Vec[Sound]] = {
    import dispatch.{Future => _, _}
    import Defaults._

    val remain  = options.maxItems - done.size
    var params  = options.toFields.iterator.map { case QueryField(key, value) => (key, value) } .toMap
//    val fields0 = if (options.previews) defaultFields + ",previews" else defaultFields
//    val fields  = if (options.images  ) fields0       + ",images"   else fields0
    val fields  = defaultFields
    params += "token"  -> client.secret
    params += "fields" -> fields
    val pageSize0 = 50
    val pageSize  = if (page == 1) math.min(remain, pageSize0) else pageSize0
    if (page > 1) params += "page" -> page.toString
    params += "page_size" -> pageSize.toString

    val req0    = url(Freesound.urlTextSearch)
    val req     = req0 <<? params
//      val req     = req1.setContentType("application/json", "UTF-8")
//      println(req.url)
    val futJson = runJSON(req) // Http(req.OK(JsonUTF))
    futJson.flatMap { json =>
      val res     = extractPage(json)
      val add     = if (res.results.size <= remain) res.results else res.results.take(remain)
      val done1   = done ++ add
      val remain1 = math.min(res.count, options.maxItems) - done1.size
      if (remain1 == 0) Future.successful(done1)
      else runTextSearch(options, page = page + 1, done = done1)
    }
  }

  def download(id: Int, out: File)(implicit auth: Auth): Processor[Unit] =
    DownloadImpl.sound(id = id, out = out, access = auth.accessToken)

  def downloadUriToFile(uri: URI, out: File)(implicit client: Client): Processor[Unit] = {
    import dispatch._
    val uriS    = uri.toString
    val req0    = url(uriS)
    val req1    = req0.addHeader("Authorization", s"Token ${client.secret}")
    val i       = uriS.lastIndexOf('/') + 1
    val info    = uriS.substring(i)
    DownloadImpl(req = req1, out = out, info = info)
  }
}