/*
 *  SampleInfoPersistentCacheImpl.scala
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

package de.sciss.freesound.impl

import java.io.{File, FileInputStream, FileOutputStream, IOException}

import de.sciss.freesound.{SampleInfo, SampleInfoCache}

import scala.util.control.NonFatal

class SampleInfoPersistentCacheImpl(path: String)
  extends SampleInfoCache {
  private val folder = new File(path)

  @throws(classOf[IOException])
  def init(): Unit = {
    if (!folder.isDirectory && !folder.mkdirs()) {
      throw new IOException(s"Could not create cache folder ($path)")
    }
  }

  def contains(id: Long): Boolean = file(id).isFile

  @throws(classOf[IOException])
  def get(id: Long): Option[SampleInfo] = {
    val f = file(id)
    try {
      val is = new FileInputStream(f)
      try {
        Some(SampleInfo.readXML(is))
      }
      catch {
        case NonFatal(_) =>
          None
      }
      finally {
        try {
          is.close()
        } catch {
          case NonFatal(_) =>
        }
      }
    }
    catch {
      case _: IOException => None
    }
  }

  @throws(classOf[IOException])
  def add(info: SampleInfo): Unit = {
    val f = file(info.id)
    if (f.exists) f.delete()
    val os = new FileOutputStream(f)
    info.writeXML(os)
    os.close()
  }

  @throws(classOf[IOException])
  def remove(id: Long): Unit =
    file(id).delete()

  private def file(id: Long): File =
    new File(folder, s"info$id.xml")
}