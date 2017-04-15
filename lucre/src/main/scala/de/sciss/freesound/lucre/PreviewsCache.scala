/*
 *  PreviewsCache.scala
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
  def acquire(sound: Sound)(implicit tx: TxnLike): Future[File]
  def release(sound: Sound)(implicit tx: TxnLike): Unit
}
