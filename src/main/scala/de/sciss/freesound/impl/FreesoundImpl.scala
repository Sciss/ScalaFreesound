package de.sciss.freesound
package impl

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object FreesoundImpl {
  def apply(token: String): Freesound = new Impl(token)

  private final class Impl(token: String) extends Freesound {
    def run(options: TextSearch): Future[String] = {
      import dispatch._, Defaults._
      val params0 = options.toFields.iterator.map { case q @ QueryField(key, value) => (key, q.encodedValue) } .toMap
      val params  = params0 + ("token" -> token)
      val req0    = host("www.freesound.org") / "apiv2" / "search" / "text"
      val req     = req0 <<? params
      println(req.url)
      val jsonS   = Http(req.OK(as.String))
      jsonS
    }
  }
}
