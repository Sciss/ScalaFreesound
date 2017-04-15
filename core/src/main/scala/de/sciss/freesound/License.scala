package de.sciss.freesound

import java.net.URI

import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.collection.breakOut

object License {
  object Unknown {
    private[freesound] final val id = 0
  }
  final case class Unknown(uri: URI) extends License {
    private[freesound] def id = Unknown.id

    override def toString: String = uri.toString
  }

  sealed trait CC extends License {
    def allowsAdaption      : Boolean
    def allowsCommercial    : Boolean
    def requiresAttribution : Boolean

    def name      : String
    def shortName : String
    def version   : Double

    def uri: URI = new URI(f"http://creativecommons.org/licenses/$uriFrag/$version%1.1f/")

    protected def uriFrag: String

    def toProperty: String

    final def | (that: LicenseExpr): LicenseExpr = LicenseExpr.fromLicense(this) | that
    final def & (that: LicenseExpr): LicenseExpr = LicenseExpr.fromLicense(this) & that

    final def unary_! : LicenseExpr = !LicenseExpr.fromLicense(this)

    override def toString: String = shortName
  }

  object CC {
    implicit object serializer extends ImmutableSerializer[CC] {
      def read(in: DataInput): CC = in.readByte() match {
        case CC0_1_0.id           => CC0_1_0
        case CC_BY_3_0.id         => CC_BY_3_0
        case CC_BY_NC_3_0.id      => CC_BY_NC_3_0
        case Sampling_Plus_1_0.id => Sampling_Plus_1_0
      }

      def write(v: CC, out: DataOutput): Unit = out.writeByte(v.id)
    }
  }

  case object CC0_1_0 extends CC {
    private[freesound] val id = 1

    def allowsAdaption      = true
    def allowsCommercial    = true
    def requiresAttribution = false

    def version = 1.0

    protected def uriFrag: String = "zero"

    override def uri: URI = new URI(f"http://creativecommons.org/publicdomain/$uriFrag/$version%1.1f/")

    def name        = "Public Domain Dedication"
    def shortName   = "CC0 1.0"
    def toProperty  = "\"Creative Commons 0\""
  }

  case object CC_BY_3_0 extends CC {
    private[freesound] val id = 2

    def allowsAdaption      = true
    def allowsCommercial    = true
    def requiresAttribution = true

    def version = 3.0

    protected def uriFrag: String = "by"

    def name        = f"Attribution $version%1.1f Unported"
    def shortName   = "CC BY 3.0"
    def toProperty  = "\"Attribution\""
  }

  case object CC_BY_NC_3_0 extends CC {
    private[freesound] val id = 3

    def allowsAdaption      = true
    def allowsCommercial    = false
    def requiresAttribution = true

    def version = 3.0

    protected def uriFrag: String = "by-nc"

    def name        = f"Attribution-NonCommercial $version%1.1f Unported"
    def shortName   = "CC BY-NC 3.0"
    def toProperty  = "\"Attribution Noncommercial\""
  }

  case object Sampling_Plus_1_0 extends CC {
    private[freesound] val id = 4

    def allowsAdaption      = true
    def allowsCommercial    = true
    def requiresAttribution = true

    def version = 1.0

    protected def uriFrag: String = "sampling+"

    def name              = f"Sampling Plus $version%1.1f"
    def shortName: String = name
    def toProperty        = "\"Sampling+\""
  }

  val known: Set[CC]    = Set(CC0_1_0, CC_BY_3_0, CC_BY_NC_3_0, Sampling_Plus_1_0)

  val map: Map[URI, CC] = known.map(lic => lic.uri -> lic)(breakOut)

  def parse(uri: URI): License = map.getOrElse(uri, Unknown(uri))

  implicit object serializer extends ImmutableSerializer[License] {
    def read(in: DataInput): License = in.readByte() match {
      case Unknown.id           => val uri = new URI(in.readUTF()); Unknown(uri)
      case CC0_1_0.id           => CC0_1_0
      case CC_BY_3_0.id         => CC_BY_3_0
      case CC_BY_NC_3_0.id      => CC_BY_NC_3_0
      case Sampling_Plus_1_0.id => Sampling_Plus_1_0
    }

    def write(v: License, out: DataOutput): Unit = v match {
      case Unknown(uri) => out.writeByte(0); out.writeUTF(uri.toString)
      case _            => out.writeByte(v.id)
    }
  }
}
sealed trait License {
  private[freesound] def id: Int

  def uri: URI
}
