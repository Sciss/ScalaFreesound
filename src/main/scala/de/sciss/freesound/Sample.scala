/*
 *  Sample.scala
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

import de.sciss.freesound.impl.SampleImpl
import de.sciss.model.Model

import scala.concurrent.Future

object Sample {

  sealed trait Update
  case object InfoBegin extends Update

  sealed abstract class InfoResult extends Update

  case class InfoDone(i: SampleInfo) extends InfoResult

  sealed abstract class InfoFailed extends InfoResult

  case object InfoFailedCurl                extends InfoFailed
  case object InfoFailedTimeout             extends InfoFailed
  case class InfoFailedParse(e: Throwable)  extends InfoFailed

  case object InfoFlushed extends Update

  case object DownloadBegin extends Update

  case class DownloadProgress(p: Int) extends Update

  sealed abstract class DownloadResult extends Update

  case class DownloadDone(path: String) extends DownloadResult

  sealed abstract class DownloadFailed  extends DownloadResult
  case object DownloadFailedCurl        extends DownloadResult
  case object DownloadFailedTimeout     extends DownloadResult

  case object DownloadFlushed extends Update

  def apply(id: Long): Sample = new SampleImpl(id)
}

trait Sample extends Model[Sample.Update] {

  import Sample._

  def id: Long

  def info: Option[SampleInfo] = infoResult.flatMap(_ match {
    case InfoDone(i) => Some(i)
    case _ => None
  })

  def info_=(value: Option[SampleInfo]): Unit

  def infoResult: Option[InfoResult]

  //   def flushInfo : Unit
  def performInfo(implicit login: Login): Unit

  def queryInfoResult: Future[InfoResult]

  def download: Option[String] = downloadResult.flatMap(_ match {
    case DownloadDone(path) => Some(path)
    case _ => None
  })

  def download_=(value: Option[String]): Unit

  def downloadResult: Option[DownloadResult]

  //   def flushDownload : Unit
  def performDownload(implicit login: Login): Unit

  def performDownload(path: String)(implicit login: Login): Unit

  def queryDownloadResult: Future[DownloadResult]
}