/*
 *  Client.scala
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

final case class Client(id: String, secret: String) {
  override def toString = s"ClientId(id = $id)"
}