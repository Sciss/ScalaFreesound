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
import java.io.{File, BufferedReader, InputStreamReader}
import actors.{OutputChannel, Future, DaemonActor}
import java.util.{Locale, Date}
import java.text.SimpleDateFormat

/**
 *    @version 0.10, 15-Jul-10
 */
object Freesound {
   val name          = "ScalaFreesound"
   val version       = 0.10
   val copyright     = "(C)opyright 2010 Hanns Holger Rutz"

   def versionString = (version + 0.001).toString.substring( 0, 4 )

   var verbose       = true
   var curlPath      = "curl"
   var tmpPath       = System.getProperty( "java.io.tmpdir" )

   var loginURL      = "http://www.freesound.org/forum/login.php"
   var searchURL     = "http://www.freesound.org/searchTextXML.php"
   var infoURL       = "http://www.freesound.org/samplesViewSingleXML.php"
   var downloadURL   = "http://www.freesound.org/samplesDownload.php"

   def search( options: SearchOptions, credentials: (String, String) ) : Search =
      new SearchImpl( options, credentials )

   def main( args: Array[ String ]) {
      printInfo
      System.exit( 1 )
   }

   def printInfo {
      println( "\n" + name + " v" + versionString + "\n" + copyright +
         ". All rights reserved.\n\nThis is a library which cannot be executed directly.\n" )
   }

   private val dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.US )

   private def unixCmd( cmd: String* )( fun: (Int, String) => Unit ) {
//      if( verbose ) println( cmd.mkString( " " ))
      
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

   private def err( text: String ) {
      println( "ERROR: " + text )
   }

   private def inform( text: String ) {
      println( text )
   }

   private object SearchImpl {
      private case object ILoginDone
      private case object ILoginTimeout
      private case class  ILoginFailed( code: Int )

      private case class  ISearchDone( ids: IIdxSeq[ Long ])
      private case object ISearchTimeout
      private case class  ISearchFailed( code: Int )
      private case class  ISearchException( cause: Throwable )
      private case object IGetResults
   }

   private class SearchImpl( val options: SearchOptions, credentials: (String, String) )
   extends DaemonActor with Search {
      search =>

      import SearchImpl._
      import Search._

      val cookiePath  = {
         val f = File.createTempFile( "cookie", ".txt", new File( tmpPath ))
         f.deleteOnExit()
         f.getCanonicalPath()
      }

      @volatile var results: Option[ IIdxSeq[ Sample ]] = None

      override def toString = "Search(" + options + " --> " + results + ")"

      // we can't use start as name because that
      // returns an actor... which is opaque in
      // the implementation
      def begin { start }

      def queryResults : Future[ Option[ IIdxSeq[ Sample ]]] = search !!( IGetResults, {
         case SearchDone( samples ) => Some( samples )
         case _ => None
      })

      private def replyLoop( res: AnyRef ) {
         dispatch( res )
         loop { react { case _ => reply( res )}}
      }

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
               replyLoop( failure )
            }
            case ILoginTimeout => {
               if( verbose ) err( "Timeout while trying to log in." )
               replyLoop( LoginFailedTimeout )
            }
            case ILoginDone => {
               if( verbose ) println( "Login was successful." )
               dispatch( LoginDone )

               retrieveResults
               if( verbose ) println( "Performing search..." )
               dispatch( SearchBegin )
               react {
                  case ISearchTimeout => {
                     if( verbose ) err( "Timeout while performing search." )
                     replyLoop( SearchFailedTimeout )
                  }
                  case ISearchFailed( code ) => {
                     if( verbose ) err( "There was an error during the search (" + code + ")." )
                     replyLoop( SearchFailedCurl )
                  }
                  case ISearchException( cause ) => {
                     if( verbose ) err( "The search results could not be parsed (" +
                        cause.getClass().getName() + " : " + cause.getMessage() + ")." )
                     replyLoop( SearchFailedParse( cause ))
                  }
                  case ISearchDone( ids ) => {
                     if( verbose ) {
                        val sz = ids.size
                        println( "Search was successful (" + sz + " sample" + (if( sz < 2 ) "" else "s") + " found)." )
                     }
                     val samples = ids.map( new SampleImpl( _, this ))
                     results  = Some( samples )
                     replyLoop( SearchDone( samples ))
                  }
               }
            }
         }
      }

      private def login {
         unixCmd( curlPath, "-c", cookiePath, "-d", "username=" + credentials._1 +
            "&password=" + credentials._2 + "&redirect=../index.php&login=login&autologin=0",
            loginURL ) { (code, response) =>
            if( code != 0 ) {
               search ! ILoginFailed( code )
            } else {
               unixCmd( curlPath, "-b", cookiePath, "-I", searchURL ) {
                  (code, response) =>
                  if( code != 0 ) {
                     search ! ILoginFailed( code )
                  } else if( response.indexOf( "text/xml" ) >= 0 ) {
                     search ! ILoginDone
                  } else {
                     search ! ILoginFailed( 0 ) // 0 indicates unexpected result
                  }
                  
               }
            }
         }
      }

      private def retrieveResults {
         unixCmd( curlPath, "-b", cookiePath, "-d", "search=" + options.keyword +
            "&start=" + options.offset + "&searchDescriptions=" + (if( options.descriptions ) 1 else 0) +
            "&searchTags=" + (if( options.tags ) 1 else 0) + "&searchFilenames=" + (if( options.fileNames ) 1 else 0) +
            "&searchUsernames=" + (if( options.userNames ) 1 else 0) + "&durationMin=" + options.minDuration +
            "&durationMax=" + options.maxDuration + "&order=" + options.order + "&limit=" + options.maxItems,
            searchURL ) { (code, response) =>

            if( code != 0 ) {
               search ! ISearchFailed( code )
            } else {
               try {
                  val elems                  = XML.loadString( response ) \ "sample"
                  val ids: IIdxSeq[ Long ]   = elems.map( e => (e \ "@id").text.toLong )( breakOut )
                  search ! ISearchDone( ids )
               }
               catch { case e =>
                  search ! ISearchException( e )
               }
            }
         }
      }

//      def numSamples = sampleIDBucket.size

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

   private object SampleImpl {
      private case object IGetInfo
      private case object IFlushInfo

      private case class  IInfoDone( i: SampleInfo )
      private case object IInfoTimeout
      private case class  IInfoFailed( code: Int )
      private case class  IInfoException( cause: Throwable )
   }

   private class SampleImpl( val id: Long, search: SearchImpl )
   extends DaemonActor with Sample {
      sample =>

      import SampleImpl._
      import Sample._

      start

      override def toString = "Sample(" + infoVar.getOrElse( id ) + ")"

      @volatile var infoVar: Option[ SampleInfo ] = None
      def info : Option[ SampleInfo ] = infoVar
      def flushInfo { sample ! IFlushInfo }

      def act {
         loop {
            react {
               case IFlushInfo => {
                  infoVar = None
                  dispatch( InfoFlushed )
               }
               case IGetInfo => {
                  val who = sender
                  if( info.isDefined ) {
                     who ! info
                  } else {
                     retrieveInfo
                     if( verbose ) println( "Getting info for sample #" + id + "..." )
                     dispatch( InfoBegin )
                     react {
                        case IInfoTimeout => {
                           if( verbose ) err( "Timeout while getting sample info #" + id + "." )
                           dispatch( InfoFailedTimeout )
                           who ! None
                        }
                        case IInfoFailed( code ) => {
                           if( verbose ) err( "Error (" + code + ") while getting sample info #" + id + "." )
                           dispatch( InfoFailedCurl )
                           who ! None
                        }
                        case IInfoException( cause ) => {
                           if( verbose ) err( "The query results for sample #" + id + " could not be parsed (" +
                              cause.getClass().getName() + " : " + cause.getMessage() + ")." )
                           dispatch( InfoFailedParse( cause ))
                           who ! None
                        }
                        case IInfoDone( i ) => {
                           if( verbose ) println( "Info for sample #" + id + " retrieved." )
                           infoVar = Some( i )
                           dispatch( InfoDone( i ))
                           who ! i
                        }
                     }
                  }
               }
            }
         }
      }

      def queryInfo : Future[ Option[ SampleInfo ]] = sample !!( IGetInfo, {
         case i: SampleInfo => Some( i )
         case _ => None
      })

      private def retrieveInfo {
//         unixCmd( curlPath, "-b", search.cookiePath, "-d", "id=" + id,
//            infoURL ) { (code, response) => }

         unixCmd( curlPath, "-b", search.cookiePath, infoURL + "?id=" + id ) { (code, response) =>
            if( code != 0 ) {
               sample ! IInfoFailed( code )
            } else {
               try {
                  val dom        = XML.loadString( response )
                  val elemSmp    = (dom \ "sample").head

                  val user       = {
                     val elemUser = (elemSmp \ "user" ).head
                     UserImpl( (elemUser \ "@id").text.toLong )( (elemUser \ "name").text )
                  }
//println( "user" )
                  val date       = dateFormat.parse( (elemSmp \ "date").text )
//println( "date" )
                  val fileName   = (elemSmp \ "originalFilename").text
                  val statistics = {
                     val elemStat   = (elemSmp \ "statistics").head
                     val elemRating = (elemStat \ "rating").head
                     StatisticsImpl( (elemStat \ "downloads").text.toInt, (elemRating \ "@count").text.toInt,
                        elemRating.text.toInt )
                  }
//println( "stats" )
                  val imageURL   = (elemSmp \ "image").text
                  val previewURL = (elemSmp \ "preview").text
                  val colorsURL  = (elemSmp \ "colors").text
                  // descriptors
//                  val descriptors = DummyDescriptors
                  // parent
                  // geotag
                  val extension  = (elemSmp \ "extension").text
                  val sampleRate = (elemSmp \ "samplerate").text.toDouble
//println( "sampleRate" )
                  val bitRate    = (elemSmp \ "bitrate").text.toInt
                  val bitDepth   = (elemSmp \ "bitdepth").text.toInt
                  val numChannels= (elemSmp \ "channels").text.toInt
//println( "numChannels" )
                  val duration   = (elemSmp \ "duration").text.toDouble
                  val fileSize   = (elemSmp \ "filesize").text.toLong
//println( "filesize" )
                  val descriptions: IIdxSeq[ Description ] =
                     (elemSmp \ "descriptions" \ "description").map( elemDescr => {
                     val elemUser   = (elemDescr \ "user" ).head
                     val user       = UserImpl( (elemUser \ "@id").text.toLong )( (elemUser \ "username").text )
                     val text       = (elemDescr \ "text").text
                     DescriptionImpl( user, text )
                  })( breakOut )
//println( "descr" )
                  val tags: ISet[ String ] = (elemSmp \ "tags" \ "tag").map( _.text )( breakOut )
                  val comments: IIdxSeq[ Comment ] = (elemSmp \ "comments" \ "comment").map( elemComment => {
                     val elemUser   = (elemComment \ "user" ).head
                     val user       = UserImpl( (elemUser \ "@id").text.toLong )( (elemUser \ "username").text )
                     val text       = (elemComment \ "text").text
                     CommentImpl( user, date, text )
                  })( breakOut )
//println( "comm" )

                  val i = SampleInfoImpl( id )(
                     user, date, fileName, statistics, imageURL, previewURL, colorsURL,
                     extension, sampleRate, bitRate, bitDepth, numChannels, duration,
                     fileSize, descriptions, tags, comments
                  )
                  sample ! IInfoDone( i )
               }
               catch { case e =>
//println( ">>>>>>>>>>>>>>>>>>>>" )
//println( response )
//println( "<<<<<<<<<<<<<<<<<<<<" )
                  sample ! IInfoException( e )
               }
            }
         }
      }
   }

   private case class UserImpl( id: Long )( val name: String ) extends User {
      override def toString = "User(" + id + ", " + name + ")"
   }

   private case class DescriptionImpl( user: User, text: String ) extends Description {
      override def toString = "Description(" + user + ", " + text + ")"
   }

   private case class CommentImpl( user: User, date: Date, text: String ) extends Comment {
      override def toString = "Comment(" + user + ", " + date + ", " + text + ")"
   }

   private case class StatisticsImpl( numDownloads: Int, numRatings: Int, rating: Int ) extends Statistics {
      override def toString = "Statistics(numDownloads = " + numDownloads + ", rating = " + rating + ")"
   }

   private case class SampleInfoImpl( id: Long )(
      val user : User,
      val date : Date,
      val fileName : String,
      val statistics : Statistics,
      val imageURL : String,
      val previewURL : String,
      val colorsURL : String,
      val extension : String,
      val sampleRate : Double,
      val bitRate : Int,
      val bitDepth : Int,
      val numChannels : Int,
      val duration : Double,
      val fileSize : Long,
      val descriptions : IIdxSeq[ Description ],
      val tags : ISet[ String ],
      val comments : IIdxSeq[ Comment ]
   ) extends SampleInfo {
      override def toString = "SampleInfo(" + id + ", " + fileName + ")"
   }
}