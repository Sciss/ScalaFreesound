/*
 *  RetrievalImpl.scala
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

package de.sciss.freesound.lucre.impl

import de.sciss.freesound.lucre.{Retrieval, TextSearchObj}
import de.sciss.lucre.Event.Targets
import de.sciss.lucre.impl.{GeneratorEvent, ObjFormat, SingleEventNode}
import de.sciss.lucre.{AnyTxn, ArtifactLocation, Copy, Elem, Folder, Obj, Pull, Txn}
import de.sciss.serial.{DataInput, DataOutput, TFormat}

object RetrievalImpl {
  def apply[T <: Txn[T]](initSearch: TextSearchObj[T], initLocation: ArtifactLocation[T])
                        (implicit tx: T): Retrieval[T] = {
    val targets           = Targets[T]()
    val textSearch        = TextSearchObj   .newVar[T](initSearch  )
    val downloadLocation  = ArtifactLocation.newVar[T](initLocation)
    val downloads         = Folder[T]()
    new Impl[T](targets, textSearch, downloadLocation, downloads).connect()
  }

  def readIdentifiedObj[T <: Txn[T]](in: DataInput)(implicit tx: T): Obj[T] = {
    val targets           = Targets.read(in)
    val c                 = in.readInt()
    require(c == COOKIE, s"Unexpected cookie (found ${c.toHexString}, expected ${COOKIE.toHexString})")
    val textSearch        = TextSearchObj   .readVar[T](in)
    val downloadLocation  = ArtifactLocation.readVar[T](in)
    val downloads         = Folder          .read   [T](in)
    new Impl[T](targets, textSearch, downloadLocation, downloads)
  }

  def format[T <: Txn[T]]: TFormat[T, Retrieval[T]] = anyFmt.asInstanceOf[Fmt[T]]

  private val anyFmt = new Fmt[AnyTxn]

  private class Fmt[T <: Txn[T]] extends ObjFormat[T, Retrieval[T]] {
    def tpe: Obj.Type = Retrieval
  }

  private final val COOKIE = 0x46535265

  private final class Impl[T <: Txn[T]](protected val targets: Targets[T],
                                        val textSearch      : TextSearchObj   .Var[T],
                                        val downloadLocation: ArtifactLocation.Var[T],
                                        val downloads       : Folder              [T])
    extends Retrieval[T] with SingleEventNode[T, Retrieval.Update[T]] { self =>

    def tpe: Obj.Type = Retrieval

    def copy[Out <: Txn[Out]]()(implicit tx: T, txOut: Out, context: Copy[T, Out]): Elem[Out] = {
      val targetsOut          = Targets[Out]()
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

    protected def disposeData()(implicit tx: T): Unit = {
      textSearch      .dispose()
      downloadLocation.dispose()
      downloads       .dispose()
    }

    def connect()(implicit tx: T): this.type = {
      textSearch      .changed ---> changed
      downloadLocation.changed ---> changed
      downloads       .changed ---> changed
      this
    }

    def disconnect()(implicit tx: T): Unit = {
      textSearch      .changed -/-> changed
      downloadLocation.changed -/-> changed
      downloads       .changed -/-> changed
    }

    object changed extends Changed
      with GeneratorEvent[T, Retrieval.Update[T]] {

      def pullUpdate(pull: Pull[T])(implicit tx: T): Option[Retrieval.Update[T]] = {
        val searchCh  = textSearch      .changed
        val locCh     = downloadLocation.changed
        val dlCh      = downloads       .changed
        val searchOpt = if (pull.contains(searchCh)) pull(searchCh) else None
        val locOpt    = if (pull.contains(locCh   )) pull(locCh   ) else None
        val dlOpt     = if (pull.contains(dlCh    )) pull(dlCh    ) else None

        var changes   = Vector.empty[Retrieval.Change[T]]
        searchOpt.foreach(changes :+= Retrieval.TextSearchChange      (_))
        locOpt   .foreach(changes :+= Retrieval.DownloadLocationChange(_))
        dlOpt    .foreach(changes :+= Retrieval.DownloadsChange       (_))

        if (changes.isEmpty) None else Some(Retrieval.Update(self, changes))
      }
    }
  }
}
