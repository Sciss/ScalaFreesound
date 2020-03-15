/*
 *  DateExpr.scala
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

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.freesound.QueryExpr.{Base, Factory}
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.annotation.switch
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

  object Option {
    import DateExpr.{serializer => valueSerializer}

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
      case 0 => val d = in.readInt(); ConstSingle(new Date(d))
      case 1 =>
        val startL  = in.readLong()
        val endL    = in.readLong()
        val start   = if (startL == 0L) scala.None else scala.Some(new Date(startL))
        val end     = if (endL   == 0L) scala.None else scala.Some(new Date(endL))
        ConstRange(start, end)
      case 2 => val a = read(in); val b = read(in); And(a, b)
      case 3 => val a = read(in); val b = read(in); Or (a, b)
      case 4 => val a = read(in);                   Not(a)
    }

    def write(v: Repr, out: DataOutput): Unit = v match {
      case ConstSingle(d)         => out.writeByte(0); out.writeLong(d.getTime)
      case ConstRange(start, end) => out.writeByte(1); out.writeLong(start.fold(0L)(_.getTime)); out.writeLong(end.fold(0L)(_.getTime))
      case And(a, b)              => out.writeByte(2); write(a, out); write(b, out)
      case Or (a, b)              => out.writeByte(3); write(a, out); write(b, out)
      case Not(a)                 => out.writeByte(4); write(a, out)
    }
  }
}
sealed trait DateExpr extends QueryExpr with DateExpr.Option {
  _: Base[DateExpr] =>

  final type Repr = DateExpr

  final private[freesound] def self: Repr with Base[Repr] = this
  final protected def factory: Factory[Repr] = DateExpr
}