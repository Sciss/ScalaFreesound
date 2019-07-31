/*
 *  ExprOps.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound

import java.util.Date

final class IntExprOps(private val i: Int) extends AnyVal {
  def to (star: *.type): UIntExpr = UIntExpr.from(i)

  def | (that: UIntExpr): UIntExpr = UIntExpr.fromInt(i) | that

  def unary_! : UIntExpr = !UIntExpr.fromInt(i)
}

final class DoubleExprOps(private val d: Double) extends AnyVal {
  def to (star: *.type): UDoubleExpr = UDoubleExpr.from(d)

  def |  (that: UDoubleExpr): UDoubleExpr = UDoubleExpr.fromDouble(d) | that

  def unary_! : UDoubleExpr = !UDoubleExpr.fromDouble(d)
}

final class DateExprOps(private val d: Date) extends AnyVal {
  def to (star: *.type): DateExpr = DateExpr.from(d)

  def | (that: DateExpr): DateExpr = DateExpr.fromDate(d) | that
}

final class StringExprOps(private val s: String) extends AnyVal {
  def | (that: StringExpr): StringExpr = StringExpr.fromString(s) | that
  def & (that: StringExpr): StringExpr = StringExpr.fromString(s) & that

  def unary_! : StringExpr = !StringExpr.fromString(s)
}

//final class LicenseExprOps(private val lic: License.CanFilter) extends AnyVal {
//  def | (that: LicenseExpr): LicenseExpr = LicenseExpr.fromLicense(lic) | that
//  def & (that: LicenseExpr): LicenseExpr = LicenseExpr.fromLicense(lic) & that
//
//  def unary_! : LicenseExpr = !LicenseExpr.fromLicense(lic)
//}