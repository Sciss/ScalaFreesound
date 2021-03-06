/*
 *  StringExpr.scala
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

import de.sciss.freesound.QueryExpr.{Base, Factory}
import de.sciss.serial.{DataInput, DataOutput, ConstFormat}

import scala.annotation.switch
import scala.language.implicitConversions

object StringExpr extends Factory[StringExpr] {
  type Repr = StringExpr

  implicit def fromString(s: String): Const = Const(s)

  implicit def fromStringOption(opt: scala.Option[String]): Option = opt.fold[Option](None)(fromString)

  implicit def fromStringSeq(xs: Seq[String]): Option =
    if (xs.isEmpty) None else xs.map(Const(_): Repr).reduce(_ | _)

  final case class Const(a: String) extends StringExpr with QueryExpr.Const[Repr] {
    def constString: String = a
  }

  final case class And(a: Repr, b: Repr) extends StringExpr with QueryExpr.And[Repr]
  final case class Or (a: Repr, b: Repr) extends StringExpr with QueryExpr.Or [Repr]
  final case class Not(a: Repr)          extends StringExpr with QueryExpr.Not[Repr]

  def and(a: Repr, b: Repr): Repr = And(a, b)
  def or (a: Repr, b: Repr): Repr = Or (a, b)
  def not(a: Repr         ): Repr = Not(a)

  object Option {
    import StringExpr.{format => valueFormat}

    implicit object format extends ConstFormat[Option] {
      def read(in: DataInput): Option = in.readByte() match {
        case 0 => None
        case 1 => valueFormat.read(in)
      }

      def write(v: Option, out: DataOutput): Unit = v match {
        case None       => out.writeByte(0)
        case some: Repr => out.writeByte(1); valueFormat.write(some, out)
      }
    }
  }
  sealed trait Option extends QueryExpr.Option {
    final type Repr = StringExpr
  }
  case object None extends Option with QueryExpr.None

  implicit object format extends ConstFormat[Repr] {
    def read(in: DataInput): Repr = (in.readByte(): @switch) match {
      case 0 => val s = in.readUTF(); Const(s)
      case 2 => val a = read(in); val b = read(in); And(a, b)
      case 3 => val a = read(in); val b = read(in); Or (a, b)
      case 4 => val a = read(in);                   Not(a)
    }

    def write(v: Repr, out: DataOutput): Unit = v match {
      case Const(s)  => out.writeByte(0); out.writeUTF(s)
      case And(a, b) => out.writeByte(2); write(a, out); write(b, out)
      case Or (a, b) => out.writeByte(3); write(a, out); write(b, out)
      case Not(a)    => out.writeByte(4); write(a, out)
    }
  }
}
sealed trait StringExpr extends QueryExpr with StringExpr.Option {
  _: Base[StringExpr] =>

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = StringExpr
}