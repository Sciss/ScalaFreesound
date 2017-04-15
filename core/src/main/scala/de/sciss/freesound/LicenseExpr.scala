/*
 *  LicenseExpr.scala
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

object LicenseExpr extends Factory[LicenseExpr] {
  type Repr = LicenseExpr

  implicit def fromLicense (tpe: License.CanFilter): Const = Const(tpe)

  implicit def fromLicenseSeq(xs: Seq[License.CanFilter]): Option =
    if (xs.isEmpty) None else xs.map(Const(_): Repr).reduce(_ | _)

  implicit def fromLicenseOption(opt: scala.Option[License.CanFilter]): Option = opt.fold[Option](None)(fromLicense)

  final case class Const(a: License.CanFilter) extends LicenseExpr with QueryExpr.Const[Repr] {
    def constString: String = a.toProperty
  }

  final case class And(a: Repr, b: Repr) extends LicenseExpr with QueryExpr.And[Repr]
  final case class Or (a: Repr, b: Repr) extends LicenseExpr with QueryExpr.Or [Repr]
  final case class Not(a: Repr)          extends LicenseExpr with QueryExpr.Not[Repr]

  def and(a: Repr, b: Repr): Repr = And(a, b)
  def or (a: Repr, b: Repr): Repr = Or (a, b)
  def not(a: Repr         ): Repr = Not(a)

  object Option {
    import LicenseExpr.{serializer => valueSerializer}

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
      case 0 => val f = License.CanFilter.serializer.read(in); Const(f)
      case 2 => val a = read(in); val b = read(in); And(a, b)
      case 3 => val a = read(in); val b = read(in); Or (a, b)
      case 4 => val a = read(in);                   Not(a)
    }

    def write(v: Repr, out: DataOutput): Unit = v match {
      case Const(f)  => out.writeByte(0); License.CanFilter.serializer.write(f, out)
      case And(a, b) => out.writeByte(2); write(a, out); write(b, out)
      case Or (a, b) => out.writeByte(3); write(a, out); write(b, out)
      case Not(a)    => out.writeByte(4); write(a, out)
    }
  }
}
sealed trait LicenseExpr extends QueryExpr with LicenseExpr.Option {
  _: Base[LicenseExpr] =>

  final type Repr = LicenseExpr

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = LicenseExpr
}