/*
 *  StringExpr.scala
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

  sealed trait Option extends QueryExpr.Option
  case object None extends Option with QueryExpr.None
}
sealed trait StringExpr extends QueryExpr with StringExpr.Option {
  _: Base[StringExpr] =>

  final type Repr = StringExpr

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = StringExpr
}