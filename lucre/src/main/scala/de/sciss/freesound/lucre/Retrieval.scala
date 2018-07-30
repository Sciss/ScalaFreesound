/*
 *  Retrieval.scala
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
package lucre

import de.sciss.freesound.lucre.impl.{RetrievalImpl => Impl}
import de.sciss.lucre.artifact.{Artifact, ArtifactLocation}
import de.sciss.lucre.event.Publisher
import de.sciss.lucre.stm.{Folder, Obj, Sys}
import de.sciss.model
import de.sciss.serial.{DataInput, Serializer}

import scala.collection.immutable.{IndexedSeq => Vec}

object Retrieval extends Obj.Type {
  final val typeId = 202

  /** Initializes all objects related to Freesound. */
  override def init(): Unit = {
    super         .init()
    SoundObj      .init()
    TextSearchObj .init()
  }

  def apply[S <: Sys[S]](initSearch: TextSearchObj[S], initLocation: ArtifactLocation[S])
                        (implicit tx: S#Tx): Retrieval[S] =
    Impl[S](initSearch = initSearch, initLocation = initLocation)

  implicit def serializer[S <: Sys[S]]: Serializer[S#Tx, S#Acc, Retrieval[S]] = Impl.serializer[S]

  def readIdentifiedObj[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Obj[S] =
    Impl.readIdentifiedObj(in, access)

  /** An update is a sequence of changes */
  final case class Update[S <: Sys[S]](r: Retrieval[S], changes: Vec[Change[S]])

  /** A change is either a state change, or a scan or a grapheme change */
  sealed trait Change[S <: Sys[S]]

  final case class TextSearchChange      [S <: Sys[S]](change: model.Change[TextSearch    ]) extends Change[S]
  final case class DownloadLocationChange[S <: Sys[S]](change: model.Change[Artifact.Value]) extends Change[S]
  final case class DownloadsChange       [S <: Sys[S]](change: Folder.Update[S])             extends Change[S]

  final val attrFreesound = "freesound"
}
trait Retrieval[S <: Sys[S]] extends Obj[S] with Publisher[S, Retrieval.Update[S]] {
  /** Last performed text search settings. */
  def textSearch: TextSearchObj.Var[S]

  /** Base directory used by the GUI for downloads. */
  def downloadLocation: ArtifactLocation.Var[S]

  /** A folder containing all the downloaded sounds.
    * Each sound (`AudioCue`) has in its attribute dictionary
    * at key `Retrieval.attrFreesound` and instance of `SoundObj`.
    */
  def downloads: Folder[S]
}
