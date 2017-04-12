/*
 *  Freesound.scala
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

import de.sciss.file._
import de.sciss.freesound.impl.{FreesoundImpl => Impl}
import de.sciss.processor.Processor

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object Freesound {
//  val dateFormat      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  val urlHome           = "https://www.freesound.org"
  val urlApiBase        = s"$urlHome/apiv2"

  /** URL for starting a text search. */
  var urlTextSearch     = s"$urlApiBase/search/text/"

  /** URL for downloading a sound.
    * A placeholder `%s` will be replaced by the sound id.
    */
  var urlSoundDownload  = s"$urlApiBase/sounds/%s/download/"

  /** URL for inspecting the sound on the Freesound website.
    * Two placeholders `%s` and `%s` will be replaced by the user-name and the sound id.
    */
  var urlSoundBrowse    = s"$urlHome/people/%s/sounds/%s/"

  var urlGetAuth        = s"$urlApiBase/oauth2/access_token/"

  /** Reads a Json file containing an object with fields
    * `id` and `secret`, specifying a client or application's
    * access keys to Freesound (http://www.freesound.org/apiv2/apply).
    */
  def readClient(f: File = file("client.json")): Client = Impl.readClient(f)

  /** Reads a Json file containing authorization access.
    */
  def readAuth(f: File = file("auth.json")): Auth = Impl.readAuth(f)

  /** Writes a Json file containing authorization access.
    */
  def writeAuth(f: File = file("auth.json"))(implicit auth: Auth): Unit = Impl.writeAuth(f)

  /** Given an authorization code obtained by the user via
    * https://www.freesound.org/apiv2/oauth2/authorize/?client_id=XXX&response_type=code
    * requests actual access keys.
    */
  def getAuth(code: String)(implicit client: Client): Future[Auth] = Impl.getAuth(code)

  /** Given an existing access key that is or might expire, requests
    * an fresh new access key.
    */
  def refreshAuth()(implicit client: Client, auth: Auth): Future[Auth] = Impl.refreshAuth()

  /** Performs a text-based search.
    *
    * @param query        the query term(s)
    * @param filter       a filter definition constraining the search results
    * @param sort         the order in which the results will be sorted
    * @param groupByPack  if `true`, groups results by sound pack (collection)
    * @param maxItems     the maximum number of result items to obtain. If the search
    *                     yields more items that this number, only the first `maxItems`
    *                     items will be received.
    */
  def textSearch(query: String, filter: Filter = Filter(), sort: Sort = Sort.Score,
          groupByPack: Boolean = false, maxItems: Int = 100)(implicit client: Client): Future[Vec[Sound]] =
    Impl.textSearch(query = query, filter = filter, sort = sort, groupByPack = groupByPack, maxItems = maxItems)

  /** Performs a text-based search and returns the total count of matches.
    *
    * @param query        the query term(s)
    * @param filter       a filter definition constraining the search results
    */
  def textCount(query: String, filter: Filter = Filter())(implicit client: Client): Future[Int] =
    Impl.textCount(query = query, filter = filter)

  /** Downloads a sound file in its original quality.
    *
    * @param id     the identifier of the sound (i.e. `Sound#id`)
    * @param out    the file to write to
    */
  def download(id: Int, out: File)(implicit auth: Auth): Processor[Unit] =
    Impl.download(id = id, out = out)
}