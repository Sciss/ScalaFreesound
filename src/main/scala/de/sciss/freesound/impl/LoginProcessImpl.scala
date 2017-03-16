/*
 *  LoginProcessImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010 Hanns Holger Rutz. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *	  Below is a copy of the GNU Lesser General Public License
 *
 *	  For further information, please contact Hanns Holger Rutz at
 *	  contact@sciss.de
 */

package de.sciss.freesound.impl

import java.io.File

import de.sciss.freesound.{Freesound, LoginProcess, Shell}
import de.sciss.model.impl.ModelImpl

import scala.concurrent.Future

object LoginProcessImpl {
  private case object ILoginDone
}

class LoginProcessImpl(val username: String, password: String)
  extends DaemonActor with LoginProcess with ModelImpl[LoginProcess.Update] {

  import Freesound.{loginURL, searchURL, tmpPath, verbose}
  import Impl._
  import LoginProcess._
  import LoginProcessImpl._

  val cookiePath = {
    val f = File.createTempFile("cookie", ".txt", new File(tmpPath))
    f.deleteOnExit()
    f.getCanonicalPath()
  }

  private lazy val loginActor: Actor = {
    start
    this
  }

  @volatile var result: Option[LoginResult] = None

  override def toString = s"LoginProcess($username)"

  // we can't use start as name because that
  // returns an actor... which is opaque in
  // the implementation
  def perform(): Unit =
    loginActor ! IPerform

  def queryResult: Future[LoginResult] = loginActor !! (IGetResult, {
    case r => r.asInstanceOf[LoginResult]
  })

  private def loopResult(res: LoginResult): Unit = {
    result = Some(res)
    dispatch(res)
    loop {
      react { case _ => reply(res) }
    }
  }

  def act {
    react { case IPerform =>
      execLogin()
      if (verbose) inform("Trying to log in...")
      dispatch(LoginBegin)
      react {
        case IFailed(code) => {
          val failure = if (code != 0) {
            if (verbose) err(s"There was an error logging in ($code).")
            LoginFailedCurl
          } else {
            if (verbose) err("Login failed, check your username and password.")
            LoginFailedCredentials
          }
          loopResult(failure)
        }
        case ITimeout => {
          if (verbose) err("Timeout while trying to log in.")
          loopResult(LoginFailedTimeout)
        }
        case ILoginDone => {
          if (verbose) println("Login was successful.")
          loopResult(LoginDone(new LoginImpl(cookiePath, username)))
        }
      }
    }
  }

  private def execLogin(): Unit = {
    Shell.curl("-c", cookiePath, "-d", "username=" + username +
      "&password=" + password + "&redirect=../index.php&login=login&autologin=0",
      loginURL) { (code, response) =>
      if (code != 0) {
        loginActor ! IFailed(code)
      } else {
        Shell.curl("-b", cookiePath, "-I", searchURL) {
          (code, response) =>
            if (code != 0) {
              loginActor ! IFailed(code)
            } else if (response.indexOf("text/xml") >= 0) {
              loginActor ! ILoginDone
            } else {
              loginActor ! IFailed(0) // 0 indicates unexpected result
            }
        }
      }
    }
  }
}
