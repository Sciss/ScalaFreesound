package de.sciss.freesound

import java.util.Date

import scala.language.implicitConversions

object Implicits {
  implicit def intRangeOps   (i: Int   ): IntRangeOps     = new IntRangeOps(i)
  implicit def doubleRangeOps(d: Double): DoubleRangeOps  = new DoubleRangeOps(d)
  implicit def dateRangeOps  (d: Date  ): DateRangeOps    = new DateRangeOps(d)
  implicit def stringUnionOps(s: String): StringUnionOps  = new StringUnionOps(s)
}