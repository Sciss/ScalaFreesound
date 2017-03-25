/*
 *  FileWithProgress.scala
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
import java.nio.ByteBuffer

import com.ning.http.client
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.resumable.ResumableAsyncHandler
import dispatch.OkHandler

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
  def apply(file: File)(progress: (Long, Long) => Unit): ResumableAsyncHandler[_] = {
    val handler = new client.resumable.ResumableAsyncHandler with OkHandler[Nothing] {
      private[this] val raf = new java.io.RandomAccessFile(file, "rw")
      if (raf.length() > 0L) raf.setLength(0L)

      private[this] var fileSize = -1L

      override def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
        val res: STATE = super.onHeadersReceived(headers)
        if (res == STATE.CONTINUE) {
          val contentLengthHeader = headers.getHeaders.getFirstValue("Content-Length")
          if (contentLengthHeader != null) {
            fileSize = java.lang.Long.parseLong(contentLengthHeader)
          }
        }
        res
      }

      setResumableListener(
        new client.extra.ResumableRandomAccessFileListener(raf) {
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