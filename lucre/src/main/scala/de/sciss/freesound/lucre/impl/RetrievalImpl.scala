/*
 *  RetrievalImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.lucre.impl

import de.sciss.freesound.lucre.{Retrieval, TextSearchObj}
import de.sciss.lucre.artifact.ArtifactLocation
import de.sciss.lucre.stm.impl.ObjSerializer
import de.sciss.lucre.stm.{Copy, Elem, Folder, NoSys, Obj, Sys}
import de.sciss.lucre.{event => evt}
import de.sciss.serial.{DataInput, DataOutput, Serializer}

object RetrievalImpl {
  def apply[S <: Sys[S]](initSearch: TextSearchObj[S], initLocation: ArtifactLocation[S])
                        (implicit tx: S#Tx): Retrieval[S] = {
    val targets           = evt.Targets[S]
    val textSearch        = TextSearchObj   .newVar[S](initSearch  )
    val downloadLocation  = ArtifactLocation.newVar[S](initLocation)
    val downloads         = Folder[S]
    new Impl[S](targets, textSearch, downloadLocation, downloads).connect()
  }

  def readIdentifiedObj[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Obj[S] = {
    val targets           = evt.Targets.read(in, access)
    val c                 = in.readInt()
    require(c == COOKIE, s"Unexpected cookie (found ${c.toHexString}, expected ${COOKIE.toHexString})")
    val textSearch        = TextSearchObj   .readVar[S](in, access)
    val downloadLocation  = ArtifactLocation.readVar[S](in, access)
    val downloads         = Folder          .read   [S](in, access)
    new Impl[S](targets, textSearch, downloadLocation, downloads)
  }

  def serializer[S <: Sys[S]]: Serializer[S#Tx, S#Acc, Retrieval[S]] = anySer.asInstanceOf[Ser[S]]

  private val anySer = new Ser[NoSys]

  private class Ser[S <: Sys[S]] extends ObjSerializer[S, Retrieval[S]] {
    def tpe: Obj.Type = Retrieval
  }

  private final val COOKIE = 0x46535265

  private final class Impl[S <: Sys[S]](protected val targets: evt.Targets[S],
                                        val textSearch      : TextSearchObj   .Var[S],
                                        val downloadLocation: ArtifactLocation.Var[S],
                                        val downloads       : Folder              [S])
    extends Retrieval[S] with evt.impl.SingleNode[S, Retrieval.Update[S]] { self =>

    def tpe: Obj.Type = Retrieval

    def copy[Out <: Sys[Out]]()(implicit tx: S#Tx, txOut: Out#Tx, context: Copy[S, Out]): Elem[Out] = {
      val targetsOut          = evt.Targets[Out]
      val textSearchOut       = context(textSearch)
      val downloadLocationOut = context(downloadLocation)
      val downloadsOut        = context(downloads)
      new Impl[Out](targetsOut, textSearchOut, downloadLocationOut, downloadsOut).connect()
    }

    protected def writeData(out: DataOutput): Unit = {
      out.writeInt(COOKIE)
      textSearch      .write(out)
      downloadLocation.write(out)
      downloads       .write(out)
    }

    protected def disposeData()(implicit tx: S#Tx): Unit = {
      textSearch      .dispose()
      downloadLocation.dispose()
      downloads       .dispose()
    }

    def connect()(implicit tx: S#Tx): this.type = {
      textSearch      .changed ---> changed
      downloadLocation.changed ---> changed
      downloads       .changed ---> changed
      this
    }

    def disconnect()(implicit tx: S#Tx): Unit = {
      textSearch      .changed -/-> changed
      downloadLocation.changed -/-> changed
      downloads       .changed -/-> changed
    }

    object changed extends Changed
      with evt.impl.Generator[S, Retrieval.Update[S]] {

      def pullUpdate(pull: evt.Pull[S])(implicit tx: S#Tx): Option[Retrieval.Update[S]] = {
        val searchCh  = textSearch      .changed
        val locCh     = downloadLocation.changed
        val dlCh      = downloads       .changed
        val searchOpt = if (pull.contains(searchCh)) pull(searchCh) else None
        val locOpt    = if (pull.contains(locCh   )) pull(locCh   ) else None
        val dlOpt     = if (pull.contains(dlCh    )) pull(dlCh    ) else None

        var changes   = Vector.empty[Retrieval.Change[S]]
        searchOpt.foreach(changes :+= Retrieval.TextSearchChange      (_))
        locOpt   .foreach(changes :+= Retrieval.DownloadLocationChange(_))
        dlOpt    .foreach(changes :+= Retrieval.DownloadsChange       (_))

        if (changes.isEmpty) None else Some(Retrieval.Update(self, changes))
      }
    }
  }
}
