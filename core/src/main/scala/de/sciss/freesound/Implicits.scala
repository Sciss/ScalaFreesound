/*
 *  Implicits.scala
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

import java.util.Date

import scala.language.implicitConversions

object Implicits {
  implicit def intExprOps    (i: Int              ): IntExprOps     = new IntExprOps(i)
  implicit def doubleExprOps (d: Double           ): DoubleExprOps  = new DoubleExprOps(d)
  implicit def dateExprOps   (d: Date             ): DateExprOps    = new DateExprOps(d)
  implicit def stringExprOps (s: String           ): StringExprOps  = new StringExprOps(s)
//  implicit def licenseExprOps(l: License.CanFilter): LicenseExprOps = new LicenseExprOps(l)
}