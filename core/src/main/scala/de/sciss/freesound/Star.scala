/*
 *  Star.scala
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

import java.util.Date

object * {
  def to (end: Int   ): UIntExpr    = UIntExpr   .to(end)
  def to (end: Double): UDoubleExpr = UDoubleExpr.to(end)
  def to (end: Date  ): DateExpr    = DateExpr   .to(end)
}