/*
 *  SampleImpl.scala
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

import xml.XML
import java.io.File
import actors.{Future, DaemonActor}
import collection.breakOut
import collection.immutable.{ IndexedSeq => IIdxSeq, Set => ISet }
import de.sciss.freesound._

object SampleImpl {
   private case class IInfoDone( i: SampleInfo )
   private case class IDownloadDone( path: String )
   private case class IPerformInfo( login: Login )
   private case class IPerformDownload( login: Login, path: Option[ String ])
}

class SampleImpl( val id: Long )
extends Sample {
   sample =>

   import SampleImpl._
   import Sample._
   import Freesound.{ verbose, tmpPath, dateFormat, infoURL, downloadURL }
   import Impl._

   override def toString = "Sample(" + id + ")"

   @volatile var infoResult: Option[ InfoResult ] = None

   private lazy val infoActor = {
      val res = new DaemonActor {
         private def loopResult( res: InfoResult ) {
            infoResult = Some( res )
            dispatch( res )
            loop { react { case _ => reply( res )}}
         }

         def act { react { case IPerformInfo( login ) =>
            execInfo( login )
            if( verbose ) println( "Getting info for sample #" + id + "..." )
            dispatch( InfoBegin )
            react {
               case ITimeout => {
                  if( verbose ) err( "Timeout while getting sample info #" + id + "." )
                  loopResult( InfoFailedTimeout )
               }
               case IFailed( code ) => {
                  if( verbose ) err( "Error (" + code + ") while getting sample info #" + id + "." )
                  loopResult( InfoFailedCurl )
               }
               case IException( cause ) => {
                  if( verbose ) err( "The query results for sample #" + id + " could not be parsed (" +
                     cause.getClass().getName() + " : " + cause.getMessage() + ")." )
                  loopResult( InfoFailedParse( cause ))
               }
               case IInfoDone( i ) => {
                  if( verbose ) println( "Info for sample #" + id + " retrieved." )
                  loopResult( InfoDone( i ))
               }
            }
         }}
      }
      res.start
      res
   }

   def flushInfo {
      // XXX abort ongoing info query?
      infoResult = None
      dispatch( InfoFlushed )
   }

   def performInfo( implicit login: Login ) { infoActor ! IPerformInfo( login )}

   def queryInfoResult : Future[ InfoResult ] = infoActor !!( IGetResult, {
      case r => r.asInstanceOf[ InfoResult ]
   })

   private def execInfo( login: Login ) {
//         unixCmd( curlPath, "-b", search.cookiePath, "-d", "id=" + id,
//            infoURL ) { (code, response) => }

      Shell.curlXML( "-b", login.cookiePath, infoURL + "?id=" + id ) { (code, response) =>
         if( code != 0 ) {
            infoActor ! IFailed( code )
         } else response match {
            case Left( xml ) => try {
                  val i = SampleInfoImpl.decodeXML( xml )
                  infoActor ! IInfoDone( i )
               }
               catch { case e => infoActor ! IException( e )}

            case Right( e ) => infoActor ! IException( e )
         }
      }
   }

   @volatile var downloadResult: Option[ DownloadResult ] = None

   private lazy val downloadActor = {
      val res = new DaemonActor {
         private def loopResult( res: DownloadResult ) {
            downloadResult = Some( res )
            dispatch( res )
            loop { react { case _ => reply( res )}}
         }

         def act { react { case IPerformDownload( login, pathOption ) =>
            execDownload( login, pathOption )
            if( verbose ) println( "Downloading sample #" + id + "..." )
            dispatch( DownloadBegin )
            react {
               case ITimeout => {
                  if( verbose ) err( "Timeout while downloading sample #" + id + "." )
                  loopResult( DownloadFailedTimeout )
               }
               case IFailed( code ) => {
                  if( verbose ) err( "Error (" + code + ") while downloading sample #" + id + "." )
                  loopResult( DownloadFailedCurl )
               }
               case IDownloadDone( path ) => {
                  if( verbose ) println( "Sample #" + id + " downloaded (" + path + ")." )
                  loopResult( DownloadDone( path ))
               }
            }
         }}
      }
      res.start
      res
   }

   def flushDownload {
      // XXX abort ongoing download?
      downloadResult = None
      dispatch( DownloadFlushed )
   }

//      private def infoGet : SampleInfo = info.getOrElse( error( "Requires info to be ready" ))

   def performDownload( implicit login: Login ) {
      downloadActor ! IPerformDownload( login, None )
   }

   def performDownload( path: String )( implicit login: Login ) {
      downloadActor ! IPerformDownload( login, Some( path ))
   }

   def queryDownloadResult : Future[ DownloadResult ] = downloadActor !!( IGetResult, {
      case r => r.asInstanceOf[ DownloadResult ]
   })

   def execDownload( login: Login, pathOption: Option[ String ]) {
      Shell.curl( "-b", login.cookiePath, "-I", downloadURL + "?id=" + id ) { (code, response) =>
         if( code != 0 ) {
            downloadActor ! IFailed( code )
         } else {
//println( ">>>>>>>>>>>>>>" )
//println( response )
//println( "<<<<<<<<<<<<<<" )
            response.split( "\n" ).find( _.startsWith( "Location:" )) map { locLine =>
               val loc     = locLine.substring( 9 ).trim
               val path    = pathOption getOrElse {
                  val fileName   = loc.substring( loc.lastIndexOf( "/" ) + 1 )
                  new File( tmpPath, fileName ).getCanonicalPath()
               }
               Shell.curlProgress( perc => dispatch( DownloadProgress( perc )),
                  "-b", login.cookiePath, loc, "-#", "-o", path ) { code =>
                  if( code != 0 ) {
                     downloadActor ! IFailed( code )
                  } else {
//         println( ">>>>>>>>>>>>>>" )
//         println( response )
//         println( "<<<<<<<<<<<<<<" )
                     downloadActor ! IDownloadDone( path )
                  }
               }
            } getOrElse { // couldn't parse header location
               downloadActor ! IFailed( 0 )
            }
         }
      }
   }
}
