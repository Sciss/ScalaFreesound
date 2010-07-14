package de.sciss.freesound

import xml.XML
import collection.breakOut

object Freesound {
   var verbose = true 

   private val sync = new AnyRef
   private var uniqueID = 0
   private var aliveInstances = Set.empty[ Freesound ]

   private def createUniqueID = sync.synchronized {
      val res = uniqueID
      uniqueID += 1
      res
   }

   private def add( fs: Freesound ) = sync.synchronized {
      aliveInstances += fs
   }

   private def unixCmd( cmd: String* )( fun: (Int, String) => Unit ) {
      error( "NOT YET IMPLEMENTED" )
   }
}

class Freesound( options: FreesoundOptions, credentials: (String, String), callback: PartialFunction[ Any, Unit ]) {
   fs =>

   import Freesound._

//   var <>keyword, credentials, searchDescriptions, searchTags, searchFileNames, searchUserNames,
//   durationMin, durationMax, order, startFrom, limit, <>verbose, >callbackFunc, <uniqueID, sampleIDBucket, numSamples;
   private val uniqueID       = createUniqueID
   private var sampleIDBucket = Vector.empty[ Int ]

   // ---- constructor ----
   {
      add( fs )

      //callbackFunc args:
      //1: trying to login
      //2: login successful
      //-2: login failed
      //3: performing search
      //4: search complete, second arg is number of results returned
      //-4: there was an error in searching.
      //5: returning sample info, second argument is info dictionary.
      //-5: there was an error getting sample info.
      //6: file downloaded. secong argument is file path.
      //-6: there was an error in downloading the file.
   }

   def doSearch {
      if( verbose ) println( "Trying to log in..." )
//      callbackFunc.value(this, 1)

      def loginCheck() {
         unixCmd( "curl", "-b", "/tmp/cookies"+uniqueID+".txt", "-I", "http://www.freesound.org/searchTextXML.php" ) {
            (code, response) =>
            if( code == 1 ) {
               if( verbose ) println( "ERROR: There was an error logging in." )
//               callbackFunc.value(this, -2)
            } else if( response.indexOf( "text/xml" ) >= 0 ) {
               if( verbose ) println( "Login was successful..." )
//               callbackFunc.value(this, 2)
               performSearch
            } else {
               if( verbose ) println( "ERROR: Login failed, check your username and password..." )
//               callbackFunc.value(this, -2)
//               halt
            }
         }
      }

      unixCmd( "curl", "-c", "/tmp/cookies"+uniqueID+".txt", "-d", "\"username=" + credentials._1 +
         "&password=" + credentials._2 + "&redirect=../index.php&login=login&autologin=0\"",
         "http://www.freesound.org/forum/login.php" ) { (code, response) =>
         if( code == 1 ) {
            if( verbose ) println( "ERROR: There was an error logging in." )
//            callbackFunc.value( this, -2)
         } else {
            loginCheck()
         }
      }
   }

   private def performSearch {
      if( verbose ) println( "Performing search..." )
//      callbackFunc.value( this, 3 )
      unixCmd( "curl", "-b", "/tmp/cookies"+uniqueID+".txt", "-d", "\"search=" + options.keyword +
         "&start=" + options.offset + "&searchDescriptions=" + (if( options.descriptions ) 1 else 0) +
         "&searchTags=" + (if( options.tags ) 1 else 0) + "&searchFilenames=" + (if( options.fileNames ) 1 else 0) +
         "&searchUsernames=" + (if( options.userNames ) 1 else 0) + "&durationMin=" + options.minDuration +
         "&durationMax=" + options.maxDuration + "&order=" + options.order + "&limit=" + options.maxItems + "\"",
         "http://www.freesound.org/searchTextXML.php" ) { (code, response) =>

         val elems = XML.loadString( response ) \ "sample"
         if( code == 1 ) {
            if( verbose ) println( "ERROR: There was an error in search." )
//            callbackFunc.value(this, -4)
         } else if( elems.isEmpty ) {
            if( verbose ) println( "ERROR: No results returned..." )
//            callbackFunc.value(this, 4, 0)
         } else {
            sampleIDBucket = elems.map( e => (e \ "@id").text.toInt )( breakOut )
            if( verbose ) println( elems.size.toString + " sample(s) returned..." )
//            callbackFunc.value(this, 4, response.size)
         }
      }
   }

   def numSamples = sampleIDBucket.size

   def getSampleInfo( index: Int ) {
      if( index >= numSamples ) {
         println( "ERROR: Sample index out of range." )
//         callbackFunc.value(this, -5)
      } else {
         if( verbose ) println( "Getting sample info #" + index + "..." )
         val id         = sampleIDBucket( index )
         val infoPath   = "/tmp/scsampleinfo"+id+"_"+uniqueID 
         unixCmd( "curl", "-b", "/tmp/cookies"+uniqueID+".txt",
            "http://www.freesound.org/samplesViewSingleXML.php?id=" + id ) { (code, response) =>

            if( code == 1 ) {
               if( verbose ) println( "ERROR: There was an error in getting sample info." )
//               callbackFunc.value(this, -5)
            } else {
               val dom = XML.loadString( response )
               val info = FreesoundInfo(
                  (dom \ "statistics" \ "downloads").text.toInt,  // numDownloads
                  (dom \ "extension").text,                       // extension
                  (dom \ "samplerate").text.toDouble,             // sampleRate
                  (dom \ "bitrate").text.toInt,                   // bitRate
                  (dom \ "bitdepth").text.toInt,                  // bitDepth
                  (dom \ "channels").text.toInt,                  // numChannels
                  (dom \ "duration").text.toDouble,               // duration
                  (dom \ "filesize").text.toLong,                 // fileSize
                  (dom \ "sample" \\ "@id").text.toLong,          // id
                  (dom \ "user" \ "@id").text.toLong,             // userID
                  (dom \ "username").text,                        // userName
                  index
               )
//            callbackFunc.value(this, 5, info)
            }
         }
      }
   }

   def downloadSample( index: Int, path: String = System.getProperty( "java.io.tmpdir" )) {
      if( verbose ) println( "Getting sample location..." )

//      if(argPath[argPath.size-1].asSymbol != '/', { argPath = argPath + "/" });

      def download( header: String, fileName: String ) {
         if( verbose ) println( "Downloading file..." )
         unixCmd( "curl", "-b", "/tmp/cookies"+uniqueID+".txt", header, ">", path + fileName ) { (code, response) =>
            if( code == 1 ) {
               if( verbose ) println( "ERROR: There was an error while trying to download file." )
//               callbackFunc.value(this, -6)
            } else {
               if( verbose ) println( "File "+fileName+" downloaded..." )
//             callbackFunc.value(this, 6, argPath+fileName)
            }
         }
      }

      val id = sampleIDBucket( index )
      val responsePath = "/tmp/scdlresponseheader"+id
      unixCmd( "curl", "-b", "/tmp/cookies"+uniqueID+".txt", "-I",
         "http://www.freesound.org/samplesDownload.php?id=" + id ) { (code, response) =>

         if( code == 1 ) {
            if( verbose ) println( "ERROR: There was an error while trying to download file." )
//            callbackFunc.value(this, -6)
         } else {
            val header     = response.replace( " ", "" ).replace( "\n", "" )
            val clean      = header.substring( header.indexOf( "Location:" ), header.indexOf( "Vary:" ))
               .replace( "\u000D", "" ) // correct? XXX
            val fileName   = clean.substring( clean.lastIndexOf( "/" ) + 1 )
            download( clean, fileName )
         }
      }
   }
}