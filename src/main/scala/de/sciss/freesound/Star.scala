package de.sciss.freesound

import java.util.Date

import de.sciss.freesound.FilterOLD.{DateSpec, UDoubleSpec, UIntSpec}

object * {
  def to (end: Int   ): UIntSpec    = UIntSpec   .to(end)
  def to (end: Double): UDoubleSpec = UDoubleSpec.to(end)
  def to (end: Date  ): DateSpec    = DateSpec   .to(end)
}
