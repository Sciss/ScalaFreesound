package de.sciss.freesound

import java.net.URLEncoder

final case class QueryField(key: String, value: String) {
  override def toString = s"$key=$value"

  def encoded: String = s"$key=${URLEncoder.encode(value, "UTF-8")}"
}
