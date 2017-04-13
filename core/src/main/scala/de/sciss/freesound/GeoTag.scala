package de.sciss.freesound

object GeoTag {
  implicit object ordering extends Ordering[GeoTag] {
    private[this] val peer = Ordering.Tuple2[Double, Double]

    def compare(x: GeoTag, y: GeoTag): Int = peer.compare(x.toTuple, y.toTuple)
  }
}

/** Geo location of a sound.
  * Warning: We have seen 'NaN's in the Freesound database.
  */
final case class GeoTag(lat: Double, lon: Double) {
  override def toString = f"$lat%1.4f, $lon%1.4f"

  def toTuple: (Double, Double) = (lat, lon)
}