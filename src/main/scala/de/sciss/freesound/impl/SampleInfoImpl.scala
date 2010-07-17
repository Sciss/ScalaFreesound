/*
 *  SampleInfoImpl.scala
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

import java.util.Date
import collection.breakOut
import collection.immutable.{ IndexedSeq => IIdxSeq, Set => ISet }
import de.sciss.freesound._
import xml.{XML, Node}
import java.io.{Writer, IOException}

/**
 *    @version 0.11, 17-Jul-10
 */
object SampleInfoImpl {
   import Freesound.dateFormat
   
   def decodeXML( xml: Node ) : SampleInfo = {
      val elemSmp    = (xml \ "sample").head
      val id         = (elemSmp \ "@id").text.toLong

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

       SampleInfoImpl( id )( xml,
         user, date, fileName, statistics, imageURL, previewURL, colorsURL,
         extension, sampleRate, bitRate, bitDepth, numChannels, duration,
         fileSize, descriptions, tags, comments
      )
   }
}

case class SampleInfoImpl( id: Long )( xml: Node,
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

   @throws( classOf[ IOException ])
   def writeXML( writer: Writer ) {
      XML.write( writer, xml, "UTF-8", true, null )
   }
}