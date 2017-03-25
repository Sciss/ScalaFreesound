/*
 *  Tokens.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound

import scala.language.implicitConversions

object AccessToken {
  implicit def apply(peer: String): AccessToken = new AccessToken(peer)
}
final class AccessToken(val peer: String) extends AnyVal

object ApiKey {
  implicit def apply(peer: String): ApiKey = new ApiKey(peer)
}
final class ApiKey(val peer: String) extends AnyVal
