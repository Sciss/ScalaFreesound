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

import impl.LoginProcessImpl
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
   var tmpPath       = System.getProperty( "java.io.tmpdir" )

   var loginURL      = "http://www.freesound.org/forum/login.php"
   var searchURL     = "http://www.freesound.org/searchTextXML.php"
   var infoURL       = "http://www.freesound.org/samplesViewSingleXML.php"
   var downloadURL   = "http://www.freesound.org/samplesDownload.php"

   val dateFormat    = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.US )

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
}