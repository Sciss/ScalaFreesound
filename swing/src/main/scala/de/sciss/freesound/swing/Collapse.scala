/*
 *  Collapse.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.swing

import java.awt.event.{ActionEvent, ActionListener}

import javax.swing.Timer

// XXX TODO --- this should go into swing-plus
object Collapse {
  def apply(delay: Int = 500, reset: Boolean = true): Collapse = new Collapse(delay, reset)
}
final class Collapse private(private var _delay: Int, reset: Boolean) extends ActionListener {
  private[this] var fun: () => Unit = () => ()

  private[this] val timer = new Timer(_delay, this)
  timer.setRepeats(false)

  def delay: Int = _delay
  def delay_=(value: Int): Unit = if (_delay != value) {
    _delay = value
    timer.setInitialDelay(value)
    if (timer.isRunning) timer.restart()
  }

  def apply(thunk: => Unit): Unit = {
    fun = () => thunk
    if (reset) tick()
  }

  def set(thunk: Unit): this.type = {
    cancel()
    fun = () => thunk
    this
  }

  def cancel(): this.type = {
    timer.stop()
    this
  }

  def tick(): this.type = {
    timer.restart()
    this
  }

  def actionPerformed(e: ActionEvent): Unit = fun()
}
