package de.sciss.freesound

final case class GeoTag(lat: Double, lon: Double) {
  override def toString = f"$lat%1.4f, $lon%1.4f"
}