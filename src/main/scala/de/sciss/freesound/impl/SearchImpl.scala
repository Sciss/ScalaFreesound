/*
 *  SearchImpl.scala
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

import actors.{Actor, DaemonActor}
import collection.breakOut
import collection.immutable.{ IndexedSeq => IIdxSeq }
import xml.XML
import de.sciss.freesound._

/**
 *    @version 0.11, 17-Jul-10
 */
object SearchImpl {
   private case class  ISearchDone( ids: IIdxSeq[ Long ])
}

class SearchImpl( val options: SearchOptions, val login: LoginImpl )
extends DaemonActor with Search {
   import SearchImpl._
   import Search._
   import Impl._
   import Freesound.{ verbose, searchURL }

   @volatile var result: Option[ SearchResult ] = None

   private lazy val searchActor : Actor = {
      start
      this
   }

   override def toString = "Search(" + options + ")"

   // we can't use start as name because that
   // returns an actor... which is opaque in
   // the implementation
   def perform { searchActor ! IPerform }

   def queryResult : Future[ SearchResult ] = searchActor !!( IGetResult, {
      case r: SearchResult => r
   })

   private def loopResult( res: SearchResult ) {
      result = Some( res )
      dispatch( res )
      loop { react { case _ => reply( res )}}
   }

   def act { react { case IPerform =>
      execSearch
      if( verbose ) println( "Performing search..." )
      dispatch( SearchBegin )
      react {
         case ITimeout => {
            if( verbose ) err( "Timeout while performing search." )
            loopResult( SearchFailedTimeout )
         }
         case IFailed( code ) => {
            if( verbose ) err( "There was an error during the search (" + code + ")." )
            loopResult( SearchFailedCurl )
         }
         case IException( cause ) => {
            if( verbose ) err( "The search results could not be parsed (" +
               cause.getClass().getName() + " : " + cause.getMessage() + ")." )
            loopResult( SearchFailedParse( cause ))
         }
         case ISearchDone( ids ) => {
            if( verbose ) {
               val sz = ids.size
               println( "Search was successful (" + sz + " sample" + (if( sz < 2 ) "" else "s") + " found)." )
            }
            val samples = ids.map( Sample( _ ))
            loopResult( SearchDone( samples ))
         }
      }
   }}

   private def execSearch {
      Shell.curlXML( "-b", login.cookiePath, "-d", "search=" + options.keyword +
         "&start=" + options.offset + "&searchDescriptions=" + (if( options.descriptions ) 1 else 0) +
         "&searchTags=" + (if( options.tags ) 1 else 0) + "&searchFilenames=" + (if( options.fileNames ) 1 else 0) +
         "&searchUsernames=" + (if( options.userNames ) 1 else 0) + "&durationMin=" + options.minDuration +
         "&durationMax=" + options.maxDuration + "&order=" + options.order + "&limit=" + options.maxItems,
         searchURL ) { (code, response) =>

         if( code != 0 ) {
            searchActor ! IFailed( code )
         } else response match {
             case Left( xml ) => try {
                  val elems                  = xml \ "sample"
                  val ids: IIdxSeq[ Long ]   = elems.map( e => (e \ "@id").text.toLong )( breakOut )
                  searchActor ! ISearchDone( ids )
               }
               catch { case e =>
                  searchActor ! IException( e )
               }

            case Right( e ) => searchActor ! IException( e )
         }
      }
   }
}