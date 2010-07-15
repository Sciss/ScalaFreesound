/*
 *  LoginProcess.scala
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

package de.sciss.freesound

import actors.Future

object LoginProcess {
   case object LoginBegin
   sealed abstract class LoginResult
   case class LoginDone( login: Login ) extends LoginResult
   sealed abstract class LoginFailed extends LoginResult
   case object LoginFailedCurl extends LoginFailed
   case object LoginFailedCredentials extends LoginFailed
   case object LoginFailedTimeout extends LoginFailed
}

trait LoginProcess extends Model {
   import LoginProcess._
   
   def perform : Unit
   def login : Option[ Login ] = result.flatMap( _ match {
      case LoginDone( l ) => Some( l )
      case _ => None
   })
   def result : Option[ LoginResult ]
   def queryResult: Future[ LoginResult ]
//   def queryLogin: Future[ Option[ Login ]]

   def username : String
//   def password : String
}