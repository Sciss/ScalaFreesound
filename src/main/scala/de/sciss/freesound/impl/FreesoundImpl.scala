package de.sciss.freesound
package impl

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object FreesoundImpl {
  def apply(token: String): Freesound = new Impl(token)

  private final class Impl(token: String) extends Freesound {
    def textSearch(options: TextSearch): Future[Vec[Sample]] = ???
  }
}
