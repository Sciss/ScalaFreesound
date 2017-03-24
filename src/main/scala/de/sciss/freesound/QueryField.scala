package de.sciss.freesound

import java.net.URLEncoder

final case class QueryField(key: String, value: String) {
  override def toString = s"$key=$value"

  def encodedValue: String = URLEncoder.encode(value, "UTF-8")

  def encoded: String = s"$key=$encodedValue"
}
