package de.sciss

import java.util.Date

import scala.language.implicitConversions

package object freesound {
  implicit def intRangeOps   (i: Int   ): IntRangeOps     = new IntRangeOps(i)
  implicit def doubleRangeOps(d: Double): DoubleRangeOps  = new DoubleRangeOps(d)
  implicit def dateRangeOps  (d: Date  ): DateRangeOps    = new DateRangeOps(d)
}