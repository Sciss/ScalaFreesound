/*
 *  FreesoundQuery.scala
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

object FreesoundQuery {
   case class Options(
      keyword: String,
      descriptions : Boolean = true,
      tags : Boolean = true,
      fileNames : Boolean = false,
      userNames : Boolean = false,
      minDuration : Int = 1,
      maxDuration : Int = 20,
      order : Int = 1,
      offset : Int = 0,
      maxItems : Int = 100
   )

   case object LoginBegin
   case object LoginDone
   sealed abstract class LoginFailed
   case object LoginFailedCurl extends LoginFailed
   case object LoginFailedCredentials extends LoginFailed
   case object LoginFailedTimeout extends LoginFailed

   case object SearchBegin
   case class SearchDone( ids: IIdxSeq[ Long ])
   sealed abstract class SearchFailed
   case object SearchFailedCurl extends SearchFailed
   case object SearchFailedTimeout extends SearchFailed
   case class SearchFailedParse( e: Throwable ) extends SearchFailed
}

trait FreesoundQuery extends Model {
   def begin : Unit
}