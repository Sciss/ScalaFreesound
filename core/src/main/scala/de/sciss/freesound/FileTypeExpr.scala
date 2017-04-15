/*
 *  FileTypeExpr.scala
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

import de.sciss.freesound.QueryExpr.{Base, Factory}
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.annotation.switch
import scala.language.implicitConversions

object FileTypeExpr extends Factory[FileTypeExpr] {
  type Repr = FileTypeExpr

  implicit def fromString   (s   : String ): Const = Const(s)
  implicit def fromFileType (tpe: FileType): Const = Const(tpe)

  implicit def fromFileTypeSeq(xs: Seq[FileType]): Option =
    if (xs.isEmpty) None else xs.map(Const(_): Repr).reduce(_ | _)

  implicit def fromFileTypeOption(opt: scala.Option[FileType]): Option = opt.fold[Option](None)(fromFileType)

  final case class Const(a: FileType) extends FileTypeExpr with QueryExpr.Const[Repr] {
    def constString: String = a.toProperty
  }

  final case class And(a: Repr, b: Repr) extends FileTypeExpr with QueryExpr.And[Repr]
  final case class Or (a: Repr, b: Repr) extends FileTypeExpr with QueryExpr.Or [Repr]
  final case class Not(a: Repr)          extends FileTypeExpr with QueryExpr.Not[Repr]

  def and(a: Repr, b: Repr): Repr = And(a, b)
  def or (a: Repr, b: Repr): Repr = Or (a, b)
  def not(a: Repr         ): Repr = Not(a)

  object Option {
    import FileTypeExpr.{serializer => valueSerializer}

    implicit object serializer extends ImmutableSerializer[Option] {
      def read(in: DataInput): Option = in.readByte() match {
        case 0 => None
        case 1 => valueSerializer.read(in)
      }

      def write(v: Option, out: DataOutput): Unit = v match {
        case None       => out.writeByte(0)
        case some: Repr => out.writeByte(1); valueSerializer.write(some, out)
      }
    }
  }
  sealed trait Option extends QueryExpr.Option
  case object None extends Option with QueryExpr.None

  implicit object serializer extends ImmutableSerializer[Repr] {
    def read(in: DataInput): Repr = (in.readByte(): @switch) match {
      case 0 => val f = FileType.serializer.read(in); Const(f)
      case 2 => val a = read(in); val b = read(in); And(a, b)
      case 3 => val a = read(in); val b = read(in); Or (a, b)
      case 4 => val a = read(in);                   Not(a)
    }

    def write(v: Repr, out: DataOutput): Unit = v match {
      case Const(f)  => out.writeByte(0); FileType.serializer.write(f, out)
      case And(a, b) => out.writeByte(2); write(a, out); write(b, out)
      case Or (a, b) => out.writeByte(3); write(a, out); write(b, out)
      case Not(a)    => out.writeByte(4); write(a, out)
    }
  }
}
sealed trait FileTypeExpr extends QueryExpr with FileTypeExpr.Option {
  _: Base[FileTypeExpr] =>

  final type Repr = FileTypeExpr

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = FileTypeExpr
}