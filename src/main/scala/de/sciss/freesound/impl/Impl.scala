/*
 *  Impl.scala
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
package impl

object Impl {

  case object ITimeout
  case class  IFailed   (code : Int)
  case class  IException(cause: Throwable)
  case object IPerform
  case object IGetResult

  def err   (text: String): Unit = println(s"ERROR: $text")
  def inform(text: String): Unit = println(text)
}