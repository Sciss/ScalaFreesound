/*
 *  SampleInfo.scala
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

package de.sciss.freesound

import java.io._
import java.util.Date

import de.sciss.freesound.impl.SampleInfoImpl

import scala.collection.immutable.{IndexedSeq => Vec, Set => ISet}
import scala.xml.XML

object SampleInfo {
  @throws(classOf[IOException])
  def readXML(in: InputStream): SampleInfo = {
    val xml = XML.load(in)
    SampleInfoImpl.decodeXML(xml)
  }
}

trait SampleInfo {
  def id: Long

  def user: User

  def date: Date

  def fileName: String

  def statistics: Statistics

  def imageURL: String

  def previewURL: String

  def colorsURL: String

  // def descriptors
  // def parent
  // def geoTag
  def extension: String

  def sampleRate: Double

  def bitRate: Int

  def bitDepth: Int

  def numChannels: Int

  def duration: Double

  def fileSize: Long

  def descriptions: Vec[Description]

  def tags: ISet[String]

  def comments: Vec[Comment]

  @throws(classOf[IOException])
  def writeXML(out: OutputStream): Unit
}