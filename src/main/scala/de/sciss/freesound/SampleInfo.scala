/*
 *  SampleInfo.scala
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

import impl.SampleInfoImpl
import java.util.Date
import collection.immutable.{ IndexedSeq => IIdxSeq, Set => ISet }
import java.io.{Reader, Writer, InputStream, IOException}
import xml.XML

/**
 *    @version 0.11, 17-Jul-10
 */
object SampleInfo {
   @throws( classOf[ IOException ])
   def readXML( reader: Reader ) : SampleInfo = {
      val xml = XML.load( reader )
      SampleInfoImpl.decodeXML( xml )
   }
}

trait SampleInfo {
   def id : Long
   def user : User
   def date : Date
   def fileName : String
   def statistics : Statistics
   def imageURL : String
   def previewURL : String
   def colorsURL : String
   // def descriptors
   // def parent
   // def geoTag
   def extension : String
   def sampleRate : Double
   def bitRate : Int
   def bitDepth : Int
   def numChannels : Int
   def duration : Double
   def fileSize : Long
   def descriptions : IIdxSeq[ Description ]
   def tags : ISet[ String ]
   def comments : IIdxSeq[ Comment ]

   @throws( classOf[ IOException ])
   def writeXML( writer: Writer ) : Unit
}