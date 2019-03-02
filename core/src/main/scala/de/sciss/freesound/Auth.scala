/*
 *  Auth.scala
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

import java.util.Date

final case class Auth(accessToken: String /* , tokenType: String */, expires: Date,
                      refreshToken: String /*, scope: String */) {

  override def toString = s"Auth(expires = $expires)"
}