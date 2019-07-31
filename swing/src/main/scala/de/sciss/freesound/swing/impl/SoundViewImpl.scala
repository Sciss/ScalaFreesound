/*
 *  SoundViewImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.swing.impl

import de.sciss.freesound.Sound
import de.sciss.freesound.swing.SoundView
import de.sciss.swingplus.GroupPanel
import de.sciss.swingplus.GroupPanel.Element

import scala.swing.{Component, Label, ScrollPane, TextArea, TextField}

object SoundViewImpl {
  def apply(): SoundView = new Impl

  private final class Impl extends SoundView {

    private[this] var _sound = Option.empty[Sound]

    def sound: Option[Sound] = _sound
    def sound_=(value: Option[Sound]): Unit = {
      _sound = value
      value match {
        case Some(s) =>
          ggId          .text = s.id            .toString
          ggFileName    .text = s.fileName
          ggTags        .text = s.tags          .mkString(" ")
          ggDescription .text = s.description
          ggUserName    .text = s.userName
          ggCreated     .text = SoundView.createdString(s.created)
          ggLicense     .text = s.license       .toString
          ggPackId      .text = if (s.packId == 0) null else s.packId.toString
          ggGeoTag      .text = s.geoTag.map(_.toString).orNull
          ggFileType    .text = s.fileType      .toString // toProperty
          ggDuration    .text = f"${s.duration}%1.1f"
          ggNumChannels .text = s.numChannels   .toString
          ggSampleRate  .text = s.sampleRate    .toString
          ggBitDepth    .text = s.bitDepth      .toString
          ggBitRate     .text = s.bitRate       .toString
          ggFileSize    .text = SoundView.fileSizeString(s.fileSize)
          ggNumDownloads.text = s.numDownloads  .toString
          ggAvgRating   .text = f"${s.avgRating}%1.1f"
          ggNumRatings  .text = s.numRatings    .toString
        case None =>
          pairs.foreach { _._2.text = null }
      }
    }

    private def label(n: String): Label = new Label(s"$n:")

    private def textField(): TextField = {
      val res = new TextField(12)
      res.editable = false
      res.peer.putClientProperty("styleId", "undecorated")
      res
    }

    private def textArea(rows: Int = 4): TextArea = {
      val res = new TextArea(rows, 12)
      res.editable  = false
      res.wordWrap  = true
      res.lineWrap  = true
      res.maximumSize = {
        val m = res.maximumSize
        m.width = math.min(640, m.width)
        m
      }
      res.preferredSize = {
        val m = res.preferredSize
        m.width = math.min(640, m.width)
        m
      }
      res
    }

    private[this] val ggId            = textField()
    private[this] val ggFileName      = textField()
    private[this] val ggTags          = textArea(rows = 2)
    private[this] val ggDescription   = textArea()
    private[this] val ggUserName      = textField()
    private[this] val ggCreated       = textField()
    private[this] val ggLicense       = textField()
    private[this] val ggPackId        = textField()
    private[this] val ggGeoTag        = textField()
    private[this] val ggFileType      = textField()
    private[this] val ggDuration      = textField()
    private[this] val ggNumChannels   = textField()
    private[this] val ggSampleRate    = textField()
    private[this] val ggBitDepth      = textField()
    private[this] val ggBitRate       = textField()
    private[this] val ggFileSize      = textField()
    private[this] val ggNumDownloads  = textField()
    private[this] val ggAvgRating     = textField()
    private[this] val ggNumRatings    = textField()

    private[this] val pairs = Seq(
      label("Id"               ) -> ggId          ,
      label("File name"        ) -> ggFileName    ,
      label("Tags"             ) -> ggTags        ,
      label("Description"      ) -> ggDescription ,
      label("User name"        ) -> ggUserName    ,
      label("Created"          ) -> ggCreated     ,
      label("License"          ) -> ggLicense     ,
      label("Pack id"          ) -> ggPackId      ,
      label("Geo tag"          ) -> ggGeoTag      ,
      label("File type"        ) -> ggFileType    ,
      label("Duration [s]"     ) -> ggDuration    ,
      label("Channels"         ) -> ggNumChannels ,
      label("Sample-rate [Hz]" ) -> ggSampleRate  ,
      label("Bit depth"        ) -> ggBitDepth    ,
      label("Bit-rate [kbps]"  ) -> ggBitRate     ,
      label("File size"        ) -> ggFileSize    ,
      label("Downloads"        ) -> ggNumDownloads,
      label("Avg. rating"      ) -> ggAvgRating   ,
      label("No. of ratings"   ) -> ggNumRatings
    )

    val component: Component = {
      val pane = new GroupPanel {
        horizontal = Seq(
          Par(pairs.map(tup => tup._1: Element): _*),
          Par(pairs.map(tup => tup._2: Element): _*)
        )
        vertical = Seq(
          pairs.map { case (lb, gg) => Par(Baseline)(lb, gg) }: _*
        )
      }

      val scroll = new ScrollPane(pane)
      scroll.peer.putClientProperty("styleId", "undecorated")
      scroll
    }
  }
}