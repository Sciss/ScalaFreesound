/*
 *  LoginImpl.scala
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

package de.sciss.freesound.impl

import de.sciss.freesound.{Login, Search, SearchOptions}

class LoginImpl(val cookiePath: String, val username: String) extends Login {
  login =>
  def search(options: SearchOptions): Search = new SearchImpl(options, login)

  override def toString = s"Login($username)"
}
