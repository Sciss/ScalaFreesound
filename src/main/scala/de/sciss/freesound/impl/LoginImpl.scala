/*
 *  LoginImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010 Hanns Holger Rutz. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *	  Below is a copy of the GNU Lesser General Public License
 *
 *	  For further information, please contact Hanns Holger Rutz at
 *	  contact@sciss.de
 */

package de.sciss.freesound.impl

import de.sciss.freesound.{Sample, Search, SearchOptions, Login}

/**
 *    @version 0.11, 17-Jul-10
 */
class LoginImpl( val cookiePath: String, val username: String ) extends Login {
   login =>
   def search( options: SearchOptions ) : Search = new SearchImpl( options, login )
//   def sample( id: Long ) : Sample = new SampleImpl( id, login )

   override def toString = "Login(" + username + ")"
}
