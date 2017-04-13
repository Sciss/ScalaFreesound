/*
 *  UDoubleExpr.scala
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

import scala.language.implicitConversions

object UDoubleExpr extends Factory[UDoubleExpr] {
  type Repr = UDoubleExpr

  implicit def fromDouble(i: Double): ConstSingle = {
    require(i >= 0, s"Unsigned double must have value >=0: $i")
    ConstSingle(i)
  }

  implicit def fromRange(r: Range): ConstRange = {
    require(r.nonEmpty  , "Range must be non-empty")
    require(r.start >= 0, s"Unsigned range must have start value >=0: $r")
    require(r.step == 1 , s"Range must be contiguous: $r")
    ConstRange(start = r.start, end = r.last)
  }

  implicit def fromRangeOption (opt: scala.Option[Range ]): Option = opt.fold[Option](None)(fromRange )

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

  sealed trait Option extends QueryExpr.Option
  case object None extends Option with QueryExpr.None
}
sealed trait UDoubleExpr extends QueryExpr with UDoubleExpr.Option {
  _: Base[UDoubleExpr] =>

  final type Repr = UDoubleExpr

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = UDoubleExpr
}