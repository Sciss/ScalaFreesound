/*
 *  SampleInfoImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.impl

import java.io.{IOException, OutputStream, OutputStreamWriter}
import java.util.Date

import de.sciss.freesound._

import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec, Set => ISet}
import scala.xml.{Node, XML}

object SampleInfoImpl {

  import Freesound.dateFormat

  def decodeXML(xml: Node): SampleInfo = {
    val elemSmp = (xml \ "sample").head
    val id = (elemSmp \ "@id").text.toLong

    val user = {
      val elemUser = (elemSmp \ "user").head
      UserImpl((elemUser \ "@id").text.toLong)((elemUser \ "name").text)
    }
    //println( "user" )
    val date = dateFormat.parse((elemSmp \ "date").text)
    //println( "date" )
    val fileName = (elemSmp \ "originalFilename").text
    val statistics = {
      val elemStat = (elemSmp \ "statistics").head
      val elemRating = (elemStat \ "rating").head
      StatisticsImpl((elemStat \ "downloads").text.toInt, (elemRating \ "@count").text.toInt,
        elemRating.text.toInt)
    }
    //println( "stats" )
    val imageURL = (elemSmp \ "image").text
    val previewURL = (elemSmp \ "preview").text
    val colorsURL = (elemSmp \ "colors").text
    // descriptors
    //                  val descriptors = DummyDescriptors
    // parent
    // geotag
    val extension = (elemSmp \ "extension").text
    val sampleRate = (elemSmp \ "samplerate").text.toDouble
    //println( "sampleRate" )
    val bitRate = (elemSmp \ "bitrate").text.toInt
    val bitDepth = (elemSmp \ "bitdepth").text.toInt
    val numChannels = (elemSmp \ "channels").text.toInt
    //println( "numChannels" )
    val duration = (elemSmp \ "duration").text.toDouble
    val fileSize = (elemSmp \ "filesize").text.toLong
    //println( "filesize" )
    val descriptions: Vec[Description] =
      (elemSmp \ "descriptions" \ "description").map(elemDescr => {
        val elemUser = (elemDescr \ "user").head
        val user = UserImpl((elemUser \ "@id").text.toLong)((elemUser \ "username").text)
        val text = (elemDescr \ "text").text
        DescriptionImpl(user, text)
      })(breakOut)
    //println( "descr" )
    val tags: ISet[String] = (elemSmp \ "tags" \ "tag").map(_.text)(breakOut)
    val comments: Vec[Comment] = (elemSmp \ "comments" \ "comment").map(elemComment => {
      val elemUser = (elemComment \ "user").head
      val user = UserImpl((elemUser \ "@id").text.toLong)((elemUser \ "username").text)
      val text = (elemComment \ "text").text
      CommentImpl(user, date, text)
    })(breakOut)
    //println( "comm" )

    SampleInfoImpl(id)(xml,
      user, date, fileName, statistics, imageURL, previewURL, colorsURL,
      extension, sampleRate, bitRate, bitDepth, numChannels, duration,
      fileSize, descriptions, tags, comments
    )
  }
}

case class SampleInfoImpl(id: Long)(xml: Node,
                                    val user: User,
                                    val date: Date,
                                    val fileName: String,
                                    val statistics: Statistics,
                                    val imageURL: String,
                                    val previewURL: String,
                                    val colorsURL: String,
                                    val extension: String,
                                    val sampleRate: Double,
                                    val bitRate: Int,
                                    val bitDepth: Int,
                                    val numChannels: Int,
                                    val duration: Double,
                                    val fileSize: Long,
                                    val descriptions: Vec[Description],
                                    val tags: ISet[String],
                                    val comments: Vec[Comment]
) extends SampleInfo {
  override def toString = s"SampleInfo($id, $fileName)"

  @throws(classOf[IOException])
  def writeXML(out: OutputStream): Unit = {
    val w = new OutputStreamWriter(out)
    XML.write(w, xml, "UTF-8", xmlDecl = true, doctype = null)
    w.flush() // important!
  }
}