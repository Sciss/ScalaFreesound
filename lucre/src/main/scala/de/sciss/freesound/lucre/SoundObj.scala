/*
 *  SoundObj.scala
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

import de.sciss.freesound
import de.sciss.freesound.Sound
import de.sciss.lucre.event.Targets
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.expr.impl.ExprTypeImpl
import de.sciss.lucre.stm.Sys
import de.sciss.serial.ImmutableSerializer

object SoundObj extends ExprTypeImpl[Sound, SoundObj] {
  import freesound.lucre.{SoundObj => Repr}

  final val typeId = 201

  final val valueSerializer: ImmutableSerializer[Sound] = Sound.serializer

  def tryParse(value: Any): Option[Sound] = value match {
    case s: Sound => Some(s)
    case _        => None
  }

  protected def mkConst[S <: Sys[S]](id: S#Id, value: A)(implicit tx: S#Tx): Const[S] =
    new _Const[S](id, value)

  protected def mkVar[S <: Sys[S]](targets: Targets[S], vr: S#Var[_Ex[S]], connect: Boolean)
                                  (implicit tx: S#Tx): Var[S] = {
    val res = new _Var[S](targets, vr)
    if (connect) res.connect()
    res
  }

  private[this] final class _Const[S <: Sys[S]](val id: S#Id, val constValue: A)
    extends ConstImpl[S] with Repr[S]

  private[this] final class _Var[S <: Sys[S]](val targets: Targets[S], val ref: S#Var[_Ex[S]])
    extends VarImpl[S] with Repr[S]
}
trait SoundObj[S <: Sys[S]] extends Expr[S, Sound]