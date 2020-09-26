/*
 *  UIntExpr.scala
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

object UIntExpr extends Factory[UIntExpr] {
  type Repr = UIntExpr

  implicit def fromInt(i: Int): ConstSingle = {
    require(i >= 0, s"Unsigned integer must have value >=0: $i")
    ConstSingle(i)
  }

  implicit def fromRange(r: Range): ConstRange = {
    require(r.nonEmpty  , "Range must be non-empty")
    require(r.start >= 0, s"Unsigned range must have start value >=0: $r")
    require(r.step == 1 , s"Range must be contiguous: $r")
    ConstRange(r.start, r.last)
  }

  implicit def fromRangeOption(opt: scala.Option[Range]): Option = opt.fold[Option](None)(fromRange)

  def from(start: Int): ConstRange = {
    require(start >= 0, s"Unsigned integer must be >= 0: $start")
    ConstRange(start = start, end = -1)
  }

  def until(stop: Int): ConstRange = {
    require(stop > 0, s"Exclusive range end must be > 0: $stop")
    ConstRange(start = -1, end = stop - 1)
  }

  def to(end: Int): ConstRange = {
    require(end >= 0, s"Inclusive range end must be >= 0: $end")
    ConstRange(start = -1, end = end)
  }

  sealed trait Const extends UIntExpr with QueryExpr.Const[Repr]

  final case class ConstSingle(a: Int) extends Const {
    def constString: String = a.toString
  }

  final case class ConstRange private(private[freesound] val start: Int, private[freesound] val end: Int)
    extends Const {

    def constString: String = {
      val startS = if (start == -1) "*" else start.toString
      val endS   = if (end   == -1) "*" else end  .toString
      s"[$startS TO $endS]"
    }
  }

  final case class And(a: Repr, b: Repr) extends UIntExpr with QueryExpr.And[Repr]
  final case class Or (a: Repr, b: Repr) extends UIntExpr with QueryExpr.Or [Repr]
  final case class Not(a: Repr)          extends UIntExpr with QueryExpr.Not[Repr]

  def and(a: Repr, b: Repr): Repr = And(a, b)
  def or (a: Repr, b: Repr): Repr = Or (a, b)
  def not(a: Repr         ): Repr = Not(a)

  object Option {
    import UIntExpr.{format => valueFormat}

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
    final type Repr = UIntExpr
  }
  case object None extends Option with QueryExpr.None

  implicit object format extends ConstFormat[Repr] {
    def read(in: DataInput): Repr = (in.readByte(): @switch) match {
      case 0 => val i = in.readInt(); ConstSingle(i)
      case 1 => val start = in.readInt(); val end = in.readInt(); ConstRange(start, end)
      case 2 => val a = read(in); val b = read(in); And(a, b)
      case 3 => val a = read(in); val b = read(in); Or (a, b)
      case 4 => val a = read(in);                   Not(a)
    }

    def write(v: Repr, out: DataOutput): Unit = v match {
      case ConstSingle(i)         => out.writeByte(0); out.writeInt(i)
      case ConstRange(start, end) => out.writeByte(1); out.writeInt(start); out.writeInt(end)
      case And(a, b)              => out.writeByte(2); write(a, out); write(b, out)
      case Or (a, b)              => out.writeByte(3); write(a, out); write(b, out)
      case Not(a)                 => out.writeByte(4); write(a, out)
    }
  }
}
sealed trait UIntExpr extends QueryExpr with UIntExpr.Option {
  _: Base[UIntExpr] =>

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = UIntExpr
}