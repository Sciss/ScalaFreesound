/*
 *  RetrievalImpl.scala
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
package impl

import de.sciss.lucre.stm.{Obj, Sys}
import de.sciss.serial.DataInput

object RetrievalImpl {
  def apply[S <: Sys[S]]: Retrieval[S] = ???

  def readIdentifiedObj[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Obj[S] = ???
}
