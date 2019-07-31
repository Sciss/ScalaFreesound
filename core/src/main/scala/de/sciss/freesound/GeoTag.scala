/*
 *  GeoTag.scala
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

import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.annotation.switch
import scala.language.implicitConversions
import scala.util.Try

object GeoTag {
  implicit object ordering extends Ordering[GeoTag] {
    private[this] val peer = Ordering.Tuple2[Double, Double]

    def compare(x: GeoTag, y: GeoTag): Int = peer.compare(x.toTuple, y.toTuple)
  }

  implicit object serializer extends ImmutableSerializer[GeoTag] {
    def write(v: GeoTag, out: DataOutput): Unit = {
      out.writeDouble(v.lat)
      out.writeDouble(v.lon)
    }

    def read(in: DataInput): GeoTag = {
      val lat = in.readDouble()
      val lon = in.readDouble()
      GeoTag(lat = lat, lon = lon)
    }
  }

  implicit def fromTuple(latLon: (Double, Double)): GeoTag = GeoTag(latLon._1, latLon._2)

  object Expr {
    implicit def fromBoolean(b: Boolean): Expr = Defined(b)

    implicit object serializer extends ImmutableSerializer[Expr] {
      import GeoTag.{serializer => GeoS}

      def write(v: Expr, out: DataOutput): Unit = v match {
        case Ignore                 => out.writeByte(0)
        case Defined     (state)    => out.writeByte(1); out.writeBoolean(state)
        case Distance    (pt , max) => out.writeByte(2); GeoS.write(pt , out); out.writeDouble(max)
        case Intersection(min, max) => out.writeByte(3); GeoS.write(min, out); GeoS.write(max, out)
        case Disjunction (min, max) => out.writeByte(4); GeoS.write(min, out); GeoS.write(max, out)
      }

      def read(in: DataInput): Expr = (in.readByte(): @switch) match {
        case 0 => Ignore
        case 1 => val state = in.readBoolean(); Defined(state)
        case 2 => val pt  = GeoS.read(in); val max = in.readDouble(); Distance    (pt , max)
        case 3 => val min = GeoS.read(in); val max = GeoS.read(in)  ; Intersection(min, max)
        case 4 => val min = GeoS.read(in); val max = GeoS.read(in)  ; Disjunction (min, max)
      }
    }
  }
  sealed trait Expr {
    def toOption: Option[Apply]
  }
  /** No filtering based on geo-tags. */
  case object Ignore extends Expr {
    def toOption: Option[Defined] = None
  }

  sealed trait Apply extends Expr {
    final def toOption: Option[Apply] = Some(this)

    def toQueryString: String
  }

  /** Only include entries that have a geo-tag defined (`state == true`) or undefined (`state == false`). */
  final case class Defined(state: Boolean) extends Apply {
    def toQueryString: String =
      s"is_geotagged:$state"
  }

  /** Only include entries who are tagged with a location within `max`
    * distance in kilometers from a given `point`.
    */
  final case class Distance(point: GeoTag, max: Double) extends Apply {
    def toQueryString: String =
      s"{!geofilt sfield=geotag pt=${point.lat},${point.lon} d=$max}"
  }

  /** Only include entries who are tagged with a location within a rectangle */
  final case class Intersection(min: GeoTag, max: GeoTag) extends Apply {
    def toQueryString: String =
      s"""geotag:"Intersects(${min.lon} ${min.lat} ${max.lon} ${max.lat})""""
  }

  /** Only include entries who are tagged with a location outside a rectangle */
  final case class Disjunction (min: GeoTag, max: GeoTag) extends Apply {
    def toQueryString: String =
      s"""geotag:"IsDisjointTo(${min.lon} ${min.lat} ${max.lon} ${max.lat})""""
  }

  /** Tries to parse a `(lat, lon)` string. */
  def unapply(s: String): Option[GeoTag] = Try {
    val Array(latS, lonS) = s.split(' ')
    val lat = /* if (latS == "nan") Double.NaN else */ latS.toDouble
    val lon = /* if (lonS == "nan") Double.NaN else */ lonS.toDouble
    GeoTag(lat = lat, lon = lon)
  } .toOption
}

/** Geo location of a sound.
  * Warning: We have seen 'NaN's in the Freesound database.
  */
final case class GeoTag(lat: Double, lon: Double) {
  override def toString = f"$lat%1.4f, $lon%1.4f"

  def toTuple: (Double, Double) = (lat, lon)

  if (lat < -90 || lat > 90 || lon < -180 || lon > 180)
    throw new IllegalArgumentException(s"Values out of range: lat $lat, lon $lon")
}