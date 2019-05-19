/*
 *  UDoubleExpr.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
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
import scala.collection.immutable.NumericRange
import scala.language.implicitConversions

object UDoubleExpr extends Factory[UDoubleExpr] {
  type Repr = UDoubleExpr

  implicit def fromDouble(i: Double): ConstSingle = {
    require(i >= 0, s"Unsigned double must have value >=0: $i")
    ConstSingle(i)
  }

  implicit def fromTuple(tup: (Double, Double)): ConstRange = {
    val (start, end) = tup
    require(start >= 0    , s"Unsigned range must have start value >=0: $start")
    require(end   >= start, "Range must be non-empty")
    ConstRange(start = start, end = end)
  }

  implicit def fromRange(r: Range): ConstRange = {
    require(r.nonEmpty  , "Range must be non-empty")
    require(r.start >= 0, s"Unsigned range must have start value >=0: $r")
    require(r.step == 1 , s"Range must be contiguous: $r")
    ConstRange(start = r.start, end = r.last)
  }

  implicit def fromRangeOption (opt: scala.Option[Range ]): Option = opt.fold[Option](None)(fromRange )

  implicit def fromPartialRange(r: Range.Partial[Double, NumericRange[Double]]): ConstRange = {
    val r1 = r.by(1.0)
    require(r1.nonEmpty  , "Range must be non-empty")
    require(r1.start >= 0, s"Unsigned range must have start value >=0: $r")
    ConstRange(start = r1.start, end = r1.end)
  }

  // avoid overloading here, so we can infer implicit conversion for `None`
//  implicit def fromDoubleOption(opt: scala.Option[Double]): Option = opt.fold[Option](None)(fromDouble)

  def from(start: Double): ConstRange = {
    require(start >= 0, s"Unsigned integer must be >= 0: $start")
    ConstRange(start = start, end = -1)
  }

  def to(end: Double): ConstRange = {
    require(end >= 0, s"Inclusive range end must be >= 0: $end")
    ConstRange(start = -1, end = end)
  }

  sealed trait Const extends UDoubleExpr with QueryExpr.Const[Repr]

  final case class ConstSingle(a: Double) extends Const {
    def constString: String = a.toString
  }

  final case class ConstRange private(private val start: Double, private val end: Double) extends Const {
    def constString: String = {
      val startS = if (start == -1) "*" else start.toString
      val endS   = if (end   == -1) "*" else end  .toString
      s"[$startS TO $endS]"
    }
  }

  final case class And(a: Repr, b: Repr) extends UDoubleExpr with QueryExpr.And[Repr]
  final case class Or (a: Repr, b: Repr) extends UDoubleExpr with QueryExpr.Or [Repr]
  final case class Not(a: Repr)          extends UDoubleExpr with QueryExpr.Not[Repr]

  def and(a: Repr, b: Repr): Repr = And(a, b)
  def or (a: Repr, b: Repr): Repr = Or (a, b)
  def not(a: Repr         ): Repr = Not(a)

  object Option {
    import UDoubleExpr.{serializer => valueSerializer}

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
  sealed trait Option extends QueryExpr.Option {
    final type Repr = UDoubleExpr
  }
  case object None extends Option with QueryExpr.None

  implicit object serializer extends ImmutableSerializer[Repr] {
    def read(in: DataInput): Repr = (in.readByte(): @switch) match {
      case 0 => val i = in.readDouble(); ConstSingle(i)
      case 1 => val start = in.readDouble(); val end = in.readDouble(); ConstRange(start, end)
      case 2 => val a = read(in); val b = read(in); And(a, b)
      case 3 => val a = read(in); val b = read(in); Or (a, b)
      case 4 => val a = read(in);                   Not(a)
    }

    def write(v: Repr, out: DataOutput): Unit = v match {
      case ConstSingle(i)         => out.writeByte(0); out.writeDouble(i)
      case ConstRange(start, end) => out.writeByte(1); out.writeDouble(start); out.writeDouble(end)
      case And(a, b)              => out.writeByte(2); write(a, out); write(b, out)
      case Or (a, b)              => out.writeByte(3); write(a, out); write(b, out)
      case Not(a)                 => out.writeByte(4); write(a, out)
    }
  }
}
sealed trait UDoubleExpr extends QueryExpr with UDoubleExpr.Option {
  _: Base[UDoubleExpr] =>

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = UDoubleExpr
}