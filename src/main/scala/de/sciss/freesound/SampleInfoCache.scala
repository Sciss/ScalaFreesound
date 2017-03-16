/*
 *  SampleInfoCache.scala
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

import java.io.IOException

import de.sciss.freesound.impl.SampleInfoPersistentCacheImpl

object SampleInfoCache {
  //   def memory : SampleInfoCache = new SampleInfoMemCacheImpl
  @throws(classOf[IOException])
  def persistent(path: String): SampleInfoCache = {
    val res = new SampleInfoPersistentCacheImpl(path)
    res.init()
    res
  }
}

trait SampleInfoCache {
  def contains(id: Long): Boolean

  @throws(classOf[IOException])
  def get(id: Long): Option[SampleInfo]

  @throws(classOf[IOException])
  def add(info: SampleInfo): Unit

  @throws(classOf[IOException])
  def remove(id: Long): Unit
}