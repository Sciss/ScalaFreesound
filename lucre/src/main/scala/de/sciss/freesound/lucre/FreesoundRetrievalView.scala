/*
 *  FreesoundRetrievalView.scala
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

import de.sciss.lucre.stm
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Sys
import de.sciss.synth.proc.AuralSystem

import scala.collection.immutable.{Seq => ISeq}

object FreesoundRetrievalView {
  def apply[S <: Sys[S]](queryInit: String = "", filterInit: Filter = Filter(), soundInit: ISeq[Sound] = Nil)
           (implicit tx: S#Tx, client: Client, previewCache: PreviewsCache,
            aural: AuralSystem, cursor: stm.Cursor[S]): FreesoundRetrievalView[S] =
    impl.FreesoundRetrievalViewImpl[S](queryInit = queryInit, filterInit = filterInit, soundInit = soundInit)
}
trait FreesoundRetrievalView[S <: stm.Sys[S]] extends View.Cursor[S]
