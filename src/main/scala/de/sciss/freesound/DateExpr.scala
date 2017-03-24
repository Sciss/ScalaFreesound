/*
 *  DateExpr.scala
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

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.freesound.QueryExpr.{Base, Factory}

import scala.language.implicitConversions

object DateExpr extends Factory[DateExpr] {
  type Repr = DateExpr

  implicit def fromDate(d: Date): ConstSingle = ConstSingle(d)

  implicit def fromDateOption(opt: scala.Option[Date]): Option = opt.fold[Option](None)(fromDate)

  def from(start: Date): ConstRange = ConstRange(startOption = scala.Some(start), endOption = scala.None     )
  def to  (end  : Date): ConstRange = ConstRange(startOption = scala.None       , endOption = scala.Some(end))

  sealed trait Const extends DateExpr with QueryExpr.Const[Repr]

  // XXX TODO --- is this the correct one for Freesound?
  private val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

  final case class ConstSingle(a: Date) extends Const {
    def constString: String = formatter.format(a)
  }

  final case class ConstRange private[freesound](startOption: scala.Option[Date], endOption: scala.Option[Date])
    extends Const {

    def constString: String = {
      val startS = startOption.fold("*")(formatter.format)
      val endS   = endOption  .fold("*")(formatter.format)
      s"[$startS TO $endS]"
    }
  }

  final case class And(a: Repr, b: Repr) extends DateExpr with QueryExpr.And[Repr]
  final case class Or (a: Repr, b: Repr) extends DateExpr with QueryExpr.Or [Repr]
  final case class Not(a: Repr)          extends DateExpr with QueryExpr.Not[Repr]

  def and(a: Repr, b: Repr): Repr = And(a, b)
  def or (a: Repr, b: Repr): Repr = Or (a, b)
  def not(a: Repr         ): Repr = Not(a)

  sealed trait Option
  case object None extends Option
}
sealed trait DateExpr extends QueryExpr with DateExpr.Option {
  _: Base[DateExpr] =>

  final type Repr = DateExpr

  final protected def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = DateExpr
}