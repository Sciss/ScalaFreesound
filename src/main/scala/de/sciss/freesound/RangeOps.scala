package de.sciss.freesound

import java.util.Date

import de.sciss.freesound.Filter.{DateSpec, UDoubleSpec, UIntSpec}

final class IntRangeOps(private val i: Int) extends AnyVal {
  def to (star: *.type): UIntSpec = UIntSpec.from(i)
}

final class DoubleRangeOps(private val i: Double) extends AnyVal {
  def to (star: *.type): UDoubleSpec = UDoubleSpec.from(i)
}

final class DateRangeOps(private val i: Date) extends AnyVal {
  def to (star: *.type): DateSpec = DateSpec.from(i)
}
