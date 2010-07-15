/*
 *  Search.scala
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

import collection.immutable.{ IndexedSeq => IIdxSeq }
import actors.Future

/**
 *    @version 0.10, 15-Jul-10
 */
object Search {
   case object LoginBegin
   case object LoginDone
   sealed abstract class LoginFailed
   case object LoginFailedCurl extends LoginFailed
   case object LoginFailedCredentials extends LoginFailed
   case object LoginFailedTimeout extends LoginFailed

   case object SearchBegin
   case class SearchDone( ids: IIdxSeq[ Sample ])
   sealed abstract class SearchFailed
   case object SearchFailedCurl extends SearchFailed
   case object SearchFailedTimeout extends SearchFailed
   case class SearchFailedParse( e: Throwable ) extends SearchFailed
}

trait Search extends Model {
   def begin : Unit
   def options : SearchOptions
   def results : Option[ IIdxSeq[ Sample ]]
   def queryResults : Future[ Option[ IIdxSeq[ Sample ]]]
}