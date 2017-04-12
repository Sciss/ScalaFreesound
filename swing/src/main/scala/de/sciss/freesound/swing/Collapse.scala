/*
 *  Collapse.scala
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
package swing

import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.Timer

final case class Collapse(delay: Int = 500, reset: Boolean = true) extends ActionListener {
  private[this] var fun: () => Unit = () => ()

  private[this] val timer = new Timer(delay, this)
  timer.setRepeats(false)

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
