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

import de.sciss.lucre.artifact.ArtifactLocation
import de.sciss.lucre.stm.{Obj, Sys}
import de.sciss.synth.proc.Folder

object Retrieval {
  def apply[S <: Sys[S]]: Retrieval[S] = ???
}
trait Retrieval[S <: Sys[S]] extends Obj[S] {
  def textSearch      : TextSearchObj.Var[S]
  def downloadLocation: ArtifactLocation.Var[S]
  def downloads       : Folder[S]
}
