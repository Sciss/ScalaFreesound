/*
 *  UIntExpr.scala
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

  final case class ConstRange private(private val start: Int, private val end: Int) extends Const {
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

  sealed trait Option
  case object None extends Option
}
sealed trait UIntExpr extends QueryExpr with UIntExpr.Option {
  _: Base[UIntExpr] =>

  final type Repr = UIntExpr

  final protected def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = UIntExpr
}