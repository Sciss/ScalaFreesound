/*
 *  PreviewsCacheImpl.scala
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
package lucre
package impl

import java.net.URI

import de.sciss.file._
import de.sciss.filecache
import de.sciss.filecache.{TxnConsumer, TxnProducer}
import de.sciss.lucre.stm.TxnLike
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.concurrent.Future

object PreviewsCacheImpl {
  implicit private object uriSerializer extends ImmutableSerializer[URI] {
    def read (in         : DataInput ): URI  = new URI(in.readUTF())
    def write(v: URI, out: DataOutput): Unit = out.writeUTF(v.toString)
  }

  def apply(dir: File, capacity: filecache.Limit)(implicit tx: TxnLike, client: Client): PreviewsCache = {
    val config = filecache.Config[URI, File]()
    config.capacity = capacity
    config.evict    = (_ /* uri */, f) => if (!f.delete()) f.deleteOnExit()
    config.space    = (_ /* uri */, f) => f.length()
    config.accept   = (_ /* uri */, f) => f.length() > 0L
    config.folder   = dir
    config.folder.mkdirs()

    val prod: TxnProducer[URI, File] = TxnProducer(config)
    val cons = TxnConsumer(prod) { uri =>
      val uriS    = uri.getPath
      val name    = uriS.substring(uriS.lastIndexOf('/') + 1)
      val out     = config.folder / name
      val proc    = Freesound.downloadUriToFile(uri, out = out)
      implicit val exec = config.executionContext
      proc.transform[File]((_: Unit) => out, { e: Throwable => config.evict(uri, out); e })
    }
    new Impl(cons)
  }

  private final class Impl(cons: TxnConsumer[URI, File])
    extends PreviewsCache {

    private def key(p: Previews): URI = p.uri(ogg = true, hq = false)

    def acquire(previews: Previews)(implicit tx: TxnLike): Future[File] =
      cons.acquire(key(previews))

    def release(previews: Previews)(implicit tx: TxnLike): Unit =
      cons.release(key(previews))
  }
}