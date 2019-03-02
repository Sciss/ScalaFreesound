/*
 *  QueryField.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound

import java.net.URLEncoder

final case class QueryField(key: String, value: String) {
  override def toString = s"$key=$value"

  def encodedValue: String = URLEncoder.encode(value, "UTF-8")

  def encoded: String = s"$key=$encodedValue"
}
