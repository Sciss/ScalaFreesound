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
import java.util.{Locale, Date}
import java.text.SimpleDateFormat
import actors.{Actor, OutputChannel, Future, DaemonActor}

/**
 *    @version 0.10, 16-Jul-10
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

   private val dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.US )

   def main( args: Array[ String ]) {
      printInfo
      System.exit( 1 )
   }

   def printInfo {
      println( "\n" + name + " v" + versionString + "\n" + copyright +
         ". All rights reserved.\n\nThis is a library which cannot be executed directly.\n" )
   }

   def login( userName: String, password: String ) : LoginProcess =
      new LoginProcessImpl( userName, password )

   private def unixCmd( cmd: String* )( fun: (Int, String) => Unit ) {
      new UnixCmd( cmd, fun, None )
   }

   private def unixCmdProg( prog: Int => Unit, cmd: String* )( fun: (Int, String) => Unit ) {
      new UnixCmd( cmd, fun, Some( prog ))
   }

   private class UnixCmd( cmd: Seq[ String ], fun: (Int, String) => Unit, prog: Option[ Int => Unit ]) {
//      if( verbose ) println( cmd.mkString( " " ))

      val pb         = new ProcessBuilder( cmd: _* )
      if( prog.isDefined ) pb.redirectErrorStream( true )
      val p          = pb.start

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
      val postActor = if( prog.isDefined ) {
         val progFun = prog.get
         new DaemonActor {
            def act {
               var isOpen  = true
               val is      = p.getInputStream()
               var bars    = 0
               var inBars  = false
               var lastBars = -1
               loopWhile( isOpen ) {
                  is.read() match {
                     case -1 => { isOpen = false; funActor ! "" }
                     case 13 => { bars = 0; inBars = true }  // cr
                     case 35 => bars += 1   // '#'
                     case 32 => if( inBars ) {    // ' '
                        inBars =  false
                        if( bars != lastBars ) {
                           lastBars = bars
                           val perc = (bars * 100) / 72
                           progFun( perc )
                        }
                     }
                     case _ =>
                  }
               }
            }
         }
      } else {
         new DaemonActor {
            def act {
               val inReader   = new BufferedReader( new InputStreamReader( p.getInputStream() ))
               var isOpen     = true
               val cBuf       = new Array[ Char ]( 256 )
               val sb         = new StringBuilder( 256 )
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

   private case object ITimeout
   private case class  IFailed( code: Int )
   private case class  IException( cause: Throwable )
   private case object IPerform
   private case object IGetResult

   private object LoginProcessImpl {
      private case object ILoginDone
   }

   private class LoginProcessImpl( val username: String, password: String )
   extends DaemonActor with LoginProcess {
      import LoginProcessImpl._
      import LoginProcess._

      val cookiePath  = {
         val f = File.createTempFile( "cookie", ".txt", new File( tmpPath ))
         f.deleteOnExit()
         f.getCanonicalPath()
      }

      private lazy val loginActor : Actor = {
         start
         this
      }

      @volatile var result: Option[ LoginResult ] = None

      override def toString = "LoginProcess(" + username + ")"

      // we can't use start as name because that
      // returns an actor... which is opaque in
      // the implementation
      def perform { loginActor ! IPerform }

      def queryResult : Future[ LoginResult ] = loginActor !!( IGetResult, {
         case r => r.asInstanceOf[ LoginResult ]
      })

      private def loopResult( res: LoginResult ) {
         result = Some( res )
         dispatch( res )
         loop { react { case _ => reply( res )}}
      }

      def act { react { case IPerform =>
         execLogin
         if( verbose ) inform( "Trying to log in..." )
         dispatch( LoginBegin )
         react {
            case IFailed( code ) => {
               val failure = if( code != 0 ) {
                  if( verbose ) err( "There was an error logging in (" + code + ")." )
                  LoginFailedCurl
               } else {
                  if( verbose ) err( "Login failed, check your username and password." )
                  LoginFailedCredentials
               }
               loopResult( failure )
            }
            case ITimeout => {
               if( verbose ) err( "Timeout while trying to log in." )
               loopResult( LoginFailedTimeout )
            }
            case ILoginDone => {
               if( verbose ) println( "Login was successful." )
               loopResult( LoginDone( new LoginImpl( cookiePath, username )))
            }
         }
      }}

      private def execLogin {
         unixCmd( curlPath, "-c", cookiePath, "-d", "username=" + username +
            "&password=" + password + "&redirect=../index.php&login=login&autologin=0",
            loginURL ) { (code, response) =>
            if( code != 0 ) {
               loginActor ! IFailed( code )
            } else {
               unixCmd( curlPath, "-b", cookiePath, "-I", searchURL ) {
                  (code, response) =>
                  if( code != 0 ) {
                     loginActor ! IFailed( code )
                  } else if( response.indexOf( "text/xml" ) >= 0 ) {
                     loginActor ! ILoginDone
                  } else {
                     loginActor ! IFailed( 0 ) // 0 indicates unexpected result
                  }
               }
            }
         }
      }
   }

   private class LoginImpl( val cookiePath: String, val username: String ) extends Login {
      login =>
      def search( options: SearchOptions ) : Search = new SearchImpl( options, login )
      def sample( id: Long ) : Sample = new SampleImpl( id, login )

      override def toString = "Login(" + username + ")"
   }

   private object SearchImpl {
      private case class  ISearchDone( ids: IIdxSeq[ Long ])
   }

   private class SearchImpl( val options: SearchOptions, val login: LoginImpl )
   extends DaemonActor with Search {
      import SearchImpl._
      import Search._

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
               val samples = ids.map( new SampleImpl( _, login ))
               loopResult( SearchDone( samples ))
            }
         }
      }}

      private def execSearch {
         unixCmd( curlPath, "-b", login.cookiePath, "-d", "search=" + options.keyword +
            "&start=" + options.offset + "&searchDescriptions=" + (if( options.descriptions ) 1 else 0) +
            "&searchTags=" + (if( options.tags ) 1 else 0) + "&searchFilenames=" + (if( options.fileNames ) 1 else 0) +
            "&searchUsernames=" + (if( options.userNames ) 1 else 0) + "&durationMin=" + options.minDuration +
            "&durationMax=" + options.maxDuration + "&order=" + options.order + "&limit=" + options.maxItems,
            searchURL ) { (code, response) =>

            if( code != 0 ) {
               searchActor ! IFailed( code )
            } else {
               try {
                  val elems                  = XML.loadString( response ) \ "sample"
                  val ids: IIdxSeq[ Long ]   = elems.map( e => (e \ "@id").text.toLong )( breakOut )
                  searchActor ! ISearchDone( ids )
               }
               catch { case e =>
                  searchActor ! IException( e )
               }
            }
         }
      }
   }

   private object SampleImpl {
      private case class IInfoDone( i: SampleInfo )
      private case class IDownloadDone( path: String )
      private case class IPerformDownload( path: Option[ String ])
   }

   private class SampleImpl( val id: Long, login: LoginImpl )
   extends Sample {
      sample =>

      import SampleImpl._
      import Sample._

      override def toString = "Sample(" + id + ")"

      @volatile var infoResult: Option[ InfoResult ] = None
      
      private lazy val infoActor = {
         val res = new DaemonActor {
            private def loopResult( res: InfoResult ) {
               infoResult = Some( res )
               dispatch( res )
               loop { react { case _ => reply( res )}}
            }

            def act { react { case IPerform =>
               execInfo
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

      def performInfo { infoActor ! IPerform }

      def queryInfoResult : Future[ InfoResult ] = infoActor !!( IGetResult, {
         case r => r.asInstanceOf[ InfoResult ]
      })

      private def execInfo {
//         unixCmd( curlPath, "-b", search.cookiePath, "-d", "id=" + id,
//            infoURL ) { (code, response) => }

         unixCmd( curlPath, "-b", login.cookiePath, infoURL + "?id=" + id ) { (code, response) =>
            if( code != 0 ) {
               infoActor ! IFailed( code )
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
                  infoActor ! IInfoDone( i )
               }
               catch { case e =>
//println( ">>>>>>>>>>>>>>>>>>>>" )
//println( response )
//println( "<<<<<<<<<<<<<<<<<<<<" )
                  infoActor ! IException( e )
               }
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

            def act { react { case IPerformDownload( pathOption ) =>
               execDownload( pathOption )
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

      def performDownload {
         downloadActor ! IPerformDownload( None )
      }

      def performDownload( path: String ) {
         downloadActor ! IPerformDownload( Some( path ))
      }

      def queryDownloadResult : Future[ DownloadResult ] = downloadActor !!( IGetResult, {
         case r => r.asInstanceOf[ DownloadResult ]
      })

      def execDownload( pathOption: Option[ String ]) {
         unixCmd( curlPath, "-b", login.cookiePath, "-I", downloadURL + "?id=" + id ) { (code, response) =>
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
                  unixCmdProg( perc => dispatch( DownloadProgress( perc )),
                     curlPath, "-b", login.cookiePath, loc, "-#", "-o", path ) { (code, _) =>
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