/*
 *  LoginProcess.scala
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

import de.sciss.model.Model

import scala.concurrent.Future

object LoginProcess {

  sealed trait Update
  case object LoginBegin extends Update

  sealed abstract class LoginResult extends Update

  case class LoginDone(login: Login)  extends LoginResult

  sealed abstract class LoginFailed   extends LoginResult
  case object LoginFailedCurl         extends LoginFailed
  case object LoginFailedCredentials  extends LoginFailed
  case object LoginFailedTimeout      extends LoginFailed

}

trait LoginProcess extends Model[LoginProcess.Update] {

  import LoginProcess._

  def perform(): Unit

  def login: Option[Login] = result.flatMap(_ match {
    case LoginDone(l) => Some(l)
    case _ => None
  })

  def result: Option[LoginResult]

  def queryResult: Future[LoginResult]

  def username: String
}