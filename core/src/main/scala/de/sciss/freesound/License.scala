package de.sciss.freesound

import java.net.URI

import scala.collection.breakOut

object License {
  final case class Unknown(uri: URI) extends License {
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

    override def toString: String = shortName
  }

  case object CC0_1_0 extends CC {
    def allowsAdaption      = true
    def allowsCommercial    = true
    def requiresAttribution = false

    def version = 1.0

    protected def uriFrag: String = "zero"

    override def uri: URI = new URI(f"http://creativecommons.org/publicdomain/$uriFrag/$version%1.1f/")

    def name      = "Public Domain Dedication"
    def shortName = "CC0 1.0"
  }

  case object CC_BY_3_0 extends CC {
    def allowsAdaption      = true
    def allowsCommercial    = true
    def requiresAttribution = true

    def version = 3.0

    protected def uriFrag: String = "by"

    def name      = f"Attribution $version%1.1f Unported"
    def shortName = "CC BY 3.0"
  }

  case object CC_BY_NC_3_0 extends CC {
    def allowsAdaption      = true
    def allowsCommercial    = false
    def requiresAttribution = true

    def version = 3.0

    protected def uriFrag: String = "by-nc"

    def name      = f"Attribution-NonCommercial $version%1.1f Unported"
    def shortName = "CC BY-NC 3.0"
  }

  case object Sampling_Plus_1_0 extends CC {
    def allowsAdaption      = true
    def allowsCommercial    = true
    def requiresAttribution = true

    def version = 1.0

    protected def uriFrag: String = "sampling+"

    def name              = f"Sampling Plus $version%1.1f"
    def shortName: String = name
  }

  val known: Set[CC]    = Set(CC0_1_0, CC_BY_3_0, CC_BY_NC_3_0, Sampling_Plus_1_0)

  val map: Map[URI, CC] = known.map(lic => lic.uri -> lic)(breakOut)

  def parse(uri: URI): License = map.getOrElse(uri, Unknown(uri))
}
sealed trait License {
  def uri: URI
}
