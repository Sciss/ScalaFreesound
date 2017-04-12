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

  sealed trait Option extends QueryExpr.Option
  case object None extends Option with QueryExpr.None
}
sealed trait FileTypeExpr extends QueryExpr with FileTypeExpr.Option {
  _: Base[FileTypeExpr] =>

  final type Repr = FileTypeExpr

  final protected def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = FileTypeExpr
}