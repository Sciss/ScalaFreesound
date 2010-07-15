/*
 *  Freesound.scala
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

import xml.XML
import collection.breakOut
import collection.immutable.{ IndexedSeq => IIdxSeq, Set => ISet }
import actors.DaemonActor
import java.io.{File, BufferedReader, InputStreamReader}

/**
 *    @version 0.10, 15-Jul-10
 */
object Freesound {
   var verbose       = true
   var curlPath      = "curl"
   var tmpPath       = System.getProperty( "java.io.tmpdir" )

   var loginURL      = "http://www.freesound.org/forum/login.php"
   var searchURL     = "http://www.freesound.org/searchTextXML.php"
   var infoURL       = "http://www.freesound.org/samplesViewSingleXML.php"
   var downloadURL   = "http://www.freesound.org/samplesDownload.php"

//   var loginTimeout  = 20000  // milliseconds

//   private val sync = new AnyRef
//   private var uniqueID = 0
//   private var aliveInstances = Set.empty[ FreesoundQuery ]

   def query( options: FreesoundQuery.Options, credentials: (String, String) ) : FreesoundQuery =
      new QueryImpl( options, credentials )

//   private def createUniqueID = sync.synchronized {
//      val res = uniqueID
//      uniqueID += 1
//      res
//   }


//   private def add( fs: FreesoundQuery ) = sync.synchronized {
//      aliveInstances += fs
//   }

   private def unixCmd( cmd: String* )( fun: (Int, String) => Unit ) {
      if( verbose ) println( cmd.mkString( " " ))
      
      val pb         = new ProcessBuilder( cmd: _* )
      val p          = pb.start
      val inReader   = new BufferedReader( new InputStreamReader( p.getInputStream() ))

      val funActor = new DaemonActor {
         def act {
            react {
               case code: Int => react {
                  case response: String => {
//                     if( verbose ) {
//                        println( "Result: " + code )
//                        println( "Response >>>>>>>>" )
//                        println( response )
//                        println( "<<<<<<<< Response" )
//                     }
                     fun( code, response )
                  }
               }
            }
         }
      }
      val postActor = new DaemonActor {
         def act {
            var isOpen  = true
            val cBuf    = new Array[ Char ]( 256 )
            val sb      = new StringBuilder( 256 )
            loopWhile( isOpen ) {
               val num  = inReader.read( cBuf )
               isOpen   = num >= 0
               if( isOpen ) {
                  sb.appendAll( cBuf, 0, num )
               } else {
                  funActor ! sb.toString()
               }
            }
         }
      }
      val processActor = new DaemonActor {
         def act = try {
            p.waitFor()
         } catch { case e: InterruptedException =>
            p.destroy()
         } finally {
            funActor ! p.exitValue
         }
      }

      funActor.start
      postActor.start
      processActor.start
   }

   private case object ILoginDone
   private case object ILoginTimeout
   private case class  ILoginFailed( code: Int )

   private case class  ISearchDone( ids: IIdxSeq[ Long ])
   private case object ISearchTimeout
   private case class  ISearchFailed( code: Int )
   private case class  ISearchException( cause: Throwable )

   private class QueryImpl( options: FreesoundQuery.Options, credentials: (String, String) )
   extends DaemonActor with FreesoundQuery {
      query =>

      import FreesoundQuery._

      private val cookiePath  = {
         val f = File.createTempFile( "cookie", "txt", new File( tmpPath ))
         f.deleteOnExit()
         f.getCanonicalPath()
      }

   //   var <>keyword, credentials, searchDescriptions, searchTags, searchFileNames, searchUserNames,
   //   durationMin, durationMax, order, startFrom, limit, <>verbose, >callbackFunc, <uniqueID, sampleIDBucket, numSamples;
//      private val uniqueID       = createUniqueID
//      private var sampleIDBucket = Vector.empty[ Int ]

      def begin { start }

      def act {
         login
         if( verbose ) inform( "Trying to log in..." )
         dispatch( LoginBegin )
         react {
            case ILoginFailed( code ) => {
               val failure = if( code != 0 ) {
                  if( verbose ) err( "There was an error logging in (" + code + ")." )
                  LoginFailedCurl
               } else {
                  if( verbose ) err( "Login failed, check your username and password." )
                  LoginFailedCredentials
               }
               dispatch( failure )
            }
            case ILoginTimeout => {
               if( verbose ) err( "Timeout while trying to log in." )
               dispatch( LoginFailedTimeout )
            }
            case ILoginDone => {
               if( verbose ) println( "Login was successful." )
               dispatch( LoginDone )

               search
               if( verbose ) println( "Performing search..." )
               dispatch( SearchBegin )
               react {
                  case ISearchTimeout => {
                     if( verbose ) err( "Timeout while performing search." )
                     dispatch( SearchFailedTimeout )
                  }
                  case ISearchFailed( code ) => {
                     if( verbose ) err( "There was an error during the search (" + code + ")." )
                     dispatch( SearchFailedCurl )
                  }
                  case ISearchException( cause ) => {
                     if( verbose ) err( "The search results could not be parsed (" +
                        cause.getClass().getName() + " : " + cause.getMessage() + ")." )
                     dispatch( SearchFailedParse( cause ))
                  }
                  case ISearchDone( ids ) => {
                     if( verbose ) {
                        val sz = ids.size
                        println( "Search was successful (" + sz + " sample" + (if( sz < 2 ) "" else "s") + " found)." )
                     }
                     dispatch( SearchDone( ids ))
                  }
               }
            }
         }
      }

      private def err( text: String ) {
         println( "ERROR: " + text )
      }

      private def inform( text: String ) {
         println( text )
      }

      private def login {
         unixCmd( curlPath, "-c", cookiePath, "-d", "username=" + credentials._1 +
            "&password=" + credentials._2 + "&redirect=../index.php&login=login&autologin=0",
            loginURL ) { (code, response) =>
            if( code != 0 ) {
               query ! ILoginFailed( code )
            } else {
               unixCmd( curlPath, "-b", cookiePath, "-I", searchURL ) {
                  (code, response) =>
                  if( code != 0 ) {
                     query ! ILoginFailed( code )
                  } else if( response.indexOf( "text/xml" ) >= 0 ) {
                     query ! ILoginDone
                  } else {
                     query ! ILoginFailed( 0 ) // 0 indicates unexpected result
                  }
               }
            }
         }
      }

      private def search {
         unixCmd( curlPath, "-b", cookiePath, "-d", "search=" + options.keyword +
            "&start=" + options.offset + "&searchDescriptions=" + (if( options.descriptions ) 1 else 0) +
            "&searchTags=" + (if( options.tags ) 1 else 0) + "&searchFilenames=" + (if( options.fileNames ) 1 else 0) +
            "&searchUsernames=" + (if( options.userNames ) 1 else 0) + "&durationMin=" + options.minDuration +
            "&durationMax=" + options.maxDuration + "&order=" + options.order + "&limit=" + options.maxItems,
            searchURL ) { (code, response) =>

            if( code != 0 ) {
               query ! ISearchFailed( code )
            } else {
               try {
                  val elems                  = XML.loadString( response ) \ "sample"
                  val ids: IIdxSeq[ Long ]   = elems.map( e => (e \ "@id").text.toLong )( breakOut )
                  query ! ISearchDone( ids )
               }
               catch { case e =>
                  query ! ISearchException( e )
               }
            }
         }
      }

//      def numSamples = sampleIDBucket.size

//      def getSampleInfo( index: Int ) {
//         if( index >= numSamples ) {
//            println( "ERROR: Sample index out of range." )
//   //         callbackFunc.value(this, -5)
//         } else {
//            if( verbose ) println( "Getting sample info #" + index + "..." )
//            val id         = sampleIDBucket( index )
//            val infoPath   = "/tmp/scsampleinfo"+id+"_"+uniqueID
//            unixCmd( curlPath, "-b", cookiePath, "-d", "id=" + id,
//               infoURL ) { (code, response) =>
//
//               if( code != 0 ) {
//                  if( verbose ) println( "ERROR: There was an error in getting sample info." )
//   //               callbackFunc.value(this, -5)
//               } else {
//                  val dom = XML.loadString( response )
//                  val info = FreesoundInfo(
//                     (dom \ "statistics" \ "downloads").text.toInt,  // numDownloads
//                     (dom \ "extension").text,                       // extension
//                     (dom \ "samplerate").text.toDouble,             // sampleRate
//                     (dom \ "bitrate").text.toInt,                   // bitRate
//                     (dom \ "bitdepth").text.toInt,                  // bitDepth
//                     (dom \ "channels").text.toInt,                  // numChannels
//                     (dom \ "duration").text.toDouble,               // duration
//                     (dom \ "filesize").text.toLong,                 // fileSize
//                     (dom \ "sample" \\ "@id").text.toLong,          // id
//                     (dom \ "user" \ "@id").text.toLong,             // userID
//                     (dom \ "username").text,                        // userName
//                     index
//                  )
//   //            callbackFunc.value(this, 5, info)
//               }
//            }
//         }
//      }
//
//      def downloadSample( index: Int, path: String = tmpDir ) {
//         if( verbose ) println( "Getting sample location..." )
//
//   //      if(argPath[argPath.size-1].asSymbol != '/', { argPath = argPath + "/" });
//
//         def download( header: String, fileName: String ) {
//            if( verbose ) println( "Downloading file..." )
//            unixCmd( curlPath, "-b", cookiePath, header, ">", path + fileName ) { (code, response) =>
//               if( code != 0 ) {
//                  if( verbose ) println( "ERROR: There was an error while trying to download file." )
//   //               callbackFunc.value(this, -6)
//               } else {
//                  if( verbose ) println( "File "+fileName+" downloaded..." )
//   //             callbackFunc.value(this, 6, argPath+fileName)
//               }
//            }
//         }
//
//         val id = sampleIDBucket( index )
//         unixCmd( curlPath, "-b", cookiePath, "-I", "-d", "id=" + id,
//            downloadURL ) { (code, response) =>
//
//            if( code != 0 ) {
//               if( verbose ) println( "ERROR: There was an error while trying to download file." )
//   //            callbackFunc.value(this, -6)
//            } else {
//               val header     = response.replace( " ", "" ).replace( "\n", "" )
//               val clean      = header.substring( header.indexOf( "Location:" ), header.indexOf( "Vary:" ))
                  /* .replace( "\u000D", "" ) */
//               val fileName   = clean.substring( clean.lastIndexOf( "/" ) + 1 )
//               download( clean, fileName )
//            }
//         }
//      }
   }
}
