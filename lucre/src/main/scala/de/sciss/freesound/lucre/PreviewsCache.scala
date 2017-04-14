package de.sciss.freesound
package lucre

import de.sciss.file.File
import de.sciss.filecache.Limit
import de.sciss.lucre.stm.TxnLike

import scala.concurrent.Future

object PreviewsCache {
  def apply(dir: File, capacity: Limit = Limit(count = 100, space = 50L * 1024 * 1024))
           (implicit tx: TxnLike, client: Client): PreviewsCache =
    impl.PreviewsCacheImpl(dir, capacity)
}
trait PreviewsCache {
  def acquire(previews: Previews)(implicit tx: TxnLike): Future[File]
  def release(previews: Previews)(implicit tx: TxnLike): Unit
}
