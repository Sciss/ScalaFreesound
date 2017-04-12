/*
 *  DownloadImpl.scala
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

import java.io.File
import java.util.{concurrent => juc}

import com.ning.http.client.{AsyncHandler, ListenableFuture, Request}
import de.sciss.model.impl.ModelImpl
import de.sciss.processor.Processor
import de.sciss.processor.impl.FutureProxy

import scala.concurrent.Promise
import scala.util.Try

object DownloadImpl {
  def apply(id: Int, out: File, access: String): Processor[Unit] = {
    import dispatch._
    import Defaults._

    val req0    = url(Freesound.urlSoundDownload.format(id))
    val req1    = req0.addHeader("Authorization", s"Bearer $access")
    if (out.isFile) out.delete()

    new Impl(id, out) {
      var progress: Double = 0.0

      private[this] val handler = FileWithProgress(out) { (pos, size) =>
        val p = (pos * 100) / size
        // println(s"progress: $p% ($pos of $size)")
        progress = p
        dispatch(Processor.Progress(this, p))
      }

      private[this] val req: (Request, AsyncHandler[_]) = req1 > handler
      private[this] val lFut: ListenableFuture[_] = Http.client.executeRequest(req._1, req._2) // XXX TODO --- this can block
      private[this] val pr    = Promise[Unit]()

      protected def peerFuture: Future[Unit] = pr.future

      lFut.addListener(
        new Runnable {
          def run(): Unit = pr.complete(Try[Unit](lFut.get()))
        },
        new juc.Executor {
          def execute(runnable: Runnable): Unit = executor.execute(runnable)
        }
      )

      def abort(): Unit = lFut.abort(Processor.Aborted())
    }
  }

  private abstract class Impl(id: Int, out: File)
    extends Processor[Unit]
    with FutureProxy[Unit]
    with ModelImpl[Processor.Update[Unit, Processor[Unit]]] {

    override def toString = s"Download($id, $out) - ${peerFuture.value}"
  }
}
