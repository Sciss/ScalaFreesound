/*
 *  FileWithProgress.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
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
import java.nio.ByteBuffer

import dispatch.OkHandler
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaders}
import org.asynchttpclient.{AsyncHandler, Response}
import org.asynchttpclient.handler.resumable.{ResumableAsyncHandler, ResumableRandomAccessFileListener}

import scala.util.control.NonFatal

/** Factory for an async-handler that writes to a file
  * and informs a `progress` monitoring function about the download progress.
  */
object FileWithProgress {
  /** @param file     the file to write to. If it exists, its contents will be erased
    *                 before writing to it.
    * @param progress a function that takes two arguments, the current file position
    *                 and the expected total file length, each time a chunk has been
    *                 downloaded.
    */
  def apply(file: File)(progress: (Long, Long) => Unit): ResumableAsyncHandler = {
    val handler: ResumableAsyncHandler = new ResumableAsyncHandler with OkHandler[Response] {
      private[this] val raf = new java.io.RandomAccessFile(file, "rw")
      if (raf.length() > 0L) raf.setLength(0L)

      private[this] var fileSize = -1L

      // cf. https://github.com/dispatch/reboot/issues/119#issuecomment-289233891
      override def onThrowable(t: Throwable): Unit = {
        super.onThrowable(t)
        try raf.close() catch { case NonFatal(_) => }
      }

      override def onHeadersReceived(headers: HttpHeaders): AsyncHandler.State = {
        val res: AsyncHandler.State = super.onHeadersReceived(headers)
        if (res == AsyncHandler.State.CONTINUE) {
          val contentLengthHeader = headers.get(HttpHeaderNames.CONTENT_LENGTH) // "Content-Length"
          if (contentLengthHeader != null) {
            fileSize = java.lang.Long.parseLong(contentLengthHeader)
          }
        }
        res
      }

      setResumableListener(
        new ResumableRandomAccessFileListener(raf) {
          override def onBytesReceived(buffer: ByteBuffer): Unit = {
            super.onBytesReceived(buffer)
            if (fileSize > 0L) {
              val pos = raf.length()
              progress(pos, fileSize)
            }
          }
        }
      )
    }
    handler
  }
}