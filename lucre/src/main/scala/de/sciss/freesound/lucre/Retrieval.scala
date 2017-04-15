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
import de.sciss.serial.DataInput
import de.sciss.synth.proc.Folder

object Retrieval extends Obj.Type {
  final val typeID = 202

  def apply[S <: Sys[S]]: Retrieval[S] = ???

  def readIdentifiedObj[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Obj[S] = ???

  final val attrFreesound = "freesound"
}
trait Retrieval[S <: Sys[S]] extends Obj[S] {
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
