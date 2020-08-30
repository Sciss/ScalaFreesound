/*
 *  FileType.scala
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

package de.sciss.freesound

import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.collection.immutable.{Seq => ISeq}
import scala.language.implicitConversions

object FileType {
  case object Wave extends FileType {
    final val toProperty = "wav"
    def isCompressed = false
  }
  case object AIFF extends FileType {
    final val toProperty  = "aif"
    final val alternative = "aiff"
    def isCompressed = false
  }
  case object Ogg  extends FileType {
    final val toProperty = "ogg"
    def isCompressed = true
  }
  case object MP3  extends FileType {
    final val toProperty = "mp3"
    def isCompressed = true
  }
  case object FLAC extends FileType {
    final val toProperty = "flac"
    def isCompressed = true
  }
  case object M4A extends FileType {
    final val toProperty = "m4a"
    def isCompressed = true
  }

  val all: ISeq[FileType] = ISeq(Wave, AIFF, Ogg, MP3, FLAC, M4A)

  implicit def fromString(s: String): FileType = s match {
    case Wave.toProperty  => Wave
    case AIFF.toProperty  => AIFF
    case AIFF.alternative => AIFF
    case Ogg .toProperty  => Ogg
    case MP3 .toProperty  => MP3
    case FLAC.toProperty  => FLAC
    case M4A .toProperty  => M4A
    case _                =>
      throw new IllegalArgumentException(s"Unsupported file type '$s' (must be one of ${all.mkString(", ")})")
  }

  implicit object serializer extends ImmutableSerializer[FileType] {
    def read(in: DataInput): FileType = fromString(in.readUTF())

    def write(v: FileType, out: DataOutput): Unit = out.writeUTF(v.toProperty)
  }
}
sealed trait FileType {
  def toProperty: String

  def isCompressed: Boolean

  def unary_! : FileTypeExpr = !FileTypeExpr.Const(this)
}