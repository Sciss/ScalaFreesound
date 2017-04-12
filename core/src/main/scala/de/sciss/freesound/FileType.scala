/*
 *  FileType.scala
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

import scala.collection.immutable.{Seq => ISeq}
import scala.language.implicitConversions

object FileType {
  case object Wave extends FileType { final val toProperty = "wav"  }
  case object AIFF extends FileType {
    final val toProperty  = "aif"
    final val alternative = "aiff"
  }
  case object Ogg  extends FileType { final val toProperty = "ogg"  }
  case object MP3  extends FileType { final val toProperty = "mp3"  }
  case object FLAC extends FileType { final val toProperty = "flac" }

  val all: ISeq[FileType] = ISeq(Wave, AIFF, Ogg, MP3, FLAC)

  implicit def fromString(s: String): FileType = s match {
    case Wave.toProperty  => Wave
    case AIFF.toProperty  => AIFF
    case AIFF.alternative => AIFF
    case Ogg .toProperty  => Ogg
    case MP3 .toProperty  => MP3
    case FLAC.toProperty  => FLAC
    case _                =>
      throw new IllegalArgumentException(s"Unsupported file type '$s' (must be one of ${all.mkString(", ")})")
  }
}
sealed trait FileType {
  def toProperty: String

  def unary_! : FileTypeExpr = !FileTypeExpr.Const(this)
}