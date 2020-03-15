/*
 *  PreviewsCache.scala
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

package de.sciss.freesound.lucre

import de.sciss.file.File
import de.sciss.filecache.Limit
import de.sciss.freesound.{Client, Sound}
import de.sciss.lucre.stm.TxnLike

import scala.concurrent.{ExecutionContext, Future}

object PreviewsCache {
  def apply(dir: File, capacity: Limit = Limit(count = 100, space = 50L * 1024 * 1024))
           (implicit tx: TxnLike, client: Client): PreviewsCache =
    impl.PreviewsCacheImpl(dir, capacity)
}
trait PreviewsCache {
  def acquire(sound: Sound)(implicit tx: TxnLike): Future[File]
  def release(sound: Sound)(implicit tx: TxnLike): Unit

  implicit def executionContext: ExecutionContext
}
