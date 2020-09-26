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
import de.sciss.lucre.Event.Targets
import de.sciss.lucre.impl.ExprTypeImpl
import de.sciss.lucre.{Expr, Ident, Txn, Var => LVar}
import de.sciss.serial.ConstFormat

object SoundObj extends ExprTypeImpl[Sound, SoundObj] {
  import freesound.lucre.{SoundObj => Repr}

  final val typeId = 201

  final val valueFormat: ConstFormat[Sound] = Sound.format

  def tryParse(value: Any): Option[Sound] = value match {
    case s: Sound => Some(s)
    case _        => None
  }

  protected def mkConst[T <: Txn[T]](id: Ident[T], value: A)(implicit tx: T): Const[T] =
    new _Const[T](id, value)

  protected def mkVar[T <: Txn[T]](targets: Targets[T], vr: LVar[T, E[T]], connect: Boolean)
                                  (implicit tx: T): Var[T] = {
    val res = new _Var[T](targets, vr)
    if (connect) res.connect()
    res
  }

  private[this] final class _Const[T <: Txn[T]](val id: Ident[T], val constValue: A)
    extends ConstImpl[T] with Repr[T]

  private[this] final class _Var[T <: Txn[T]](val targets: Targets[T], val ref: LVar[T, E[T]])
    extends VarImpl[T] with Repr[T]
}
trait SoundObj[T <: Txn[T]] extends Expr[T, Sound]