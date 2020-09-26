/*
 *  Retrieval.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.lucre

import de.sciss.freesound.TextSearch
import de.sciss.freesound.lucre.impl.{RetrievalImpl => Impl}
import de.sciss.lucre.{Artifact, ArtifactLocation}
import de.sciss.lucre.Publisher
import de.sciss.lucre.{Folder, Obj, Txn}
import de.sciss.model
import de.sciss.serial.{DataInput, TFormat}

import scala.collection.immutable.{IndexedSeq => Vec}

object Retrieval extends Obj.Type {
  final val typeId = 202

  /** Initializes all objects related to Freesound. */
  override def init(): Unit = {
    super         .init()
    SoundObj      .init()
    TextSearchObj .init()
  }

  def apply[T <: Txn[T]](initSearch: TextSearchObj[T], initLocation: ArtifactLocation[T])
                        (implicit tx: T): Retrieval[T] =
    Impl[T](initSearch = initSearch, initLocation = initLocation)

  implicit def format[T <: Txn[T]]: TFormat[T, Retrieval[T]] = Impl.format[T]

  def readIdentifiedObj[T <: Txn[T]](in: DataInput)(implicit tx: T): Obj[T] =
    Impl.readIdentifiedObj(in)

  /** An update is a sequence of changes */
  final case class Update[T <: Txn[T]](r: Retrieval[T], changes: Vec[Change[T]])

  /** A change is either a state change, or a scan or a grapheme change */
  sealed trait Change[T <: Txn[T]]

  final case class TextSearchChange      [T <: Txn[T]](change: model.Change[TextSearch    ]) extends Change[T]
  final case class DownloadLocationChange[T <: Txn[T]](change: model.Change[Artifact.Value]) extends Change[T]
  final case class DownloadsChange       [T <: Txn[T]](change: Folder.Update[T])             extends Change[T]

  final val attrFreesound = "freesound"
}
trait Retrieval[T <: Txn[T]] extends Obj[T] with Publisher[T, Retrieval.Update[T]] {
  /** Last performed text search settings. */
  def textSearch: TextSearchObj.Var[T]

  /** Base directory used by the GUI for downloads. */
  def downloadLocation: ArtifactLocation.Var[T]

  /** A folder containing all the downloaded sounds.
    * Each sound (`AudioCue`) has in its attribute dictionary
    * at key `Retrieval.attrFreesound` and instance of `SoundObj`.
    */
  def downloads: Folder[T]
}
