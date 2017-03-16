/*
 *  Shell.scala
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

import java.io.{BufferedReader, InputStreamReader}

import scala.util.control.NonFatal
import scala.xml.{Node, XML}

object Shell {
  var curlPath = "curl"

  def command(cmd: String*)(fun: (Int, String) => Unit): Unit =
    new Command(cmd, StringPost, new ResultActor(fun))

  def curl(args: String*)(fun: (Int, String) => Unit): Unit =
    new Command(curlPath :: args.toList, StringPost, new ResultActor(fun))

  def curlXML(args: String*)(fun: (Int, Either[Node, Throwable]) => Unit): Unit =
    new Command(curlPath :: args.toList, XMLPost, new ResultActor(fun))

  def curlProgress(prog: Int => Unit, args: String*)(fun: Int => Unit): Unit =
    new Command(curlPath :: args.toList, new CurlProgressPostFactory(prog),
      new ResultActor((code: Int, _: Unit) => fun(code)))

  private class Command[A](cmd: Seq[String], post: PostActorFactory[A], resultActor: ResultActor[A]) {

    val pb          = new ProcessBuilder(cmd: _*)
    val p: Process  = pb.start

    val postActor: Any = post.make(p, resultActor)

    val processActor = new DaemonActor {
      def act = try {
        p.waitFor()
      } catch {
        case e: InterruptedException =>
          p.destroy()
      } finally {
        resultActor ! Code(p.exitValue)
      }
    }

    resultActor .start
    postActor   .start
    processActor.start
  }

  private class ResultActor[A](fun: (Int, A) => Unit)
    extends DaemonActor {
    def act {
      react {
        case Code(code) => react {
          case response =>
            fun(code, response.asInstanceOf[A]) // XXX hmmm... not so nice the casting...
        }
      }
    }
  }

  private case class Code(value: Int)

  private trait PostActorFactory[A] {
    def make(p: Process, result: ResultActor[A]): Actor
  }

  private object StringPost extends PostActorFactory[String] {
    def make(p: Process, result: ResultActor[String]) = new StringPost(p, result)
  }

  private class StringPost(p: Process, result: ResultActor[String]) extends DaemonActor {
    def act {
      val inReader  = new BufferedReader(new InputStreamReader(p.getInputStream))
      var isOpen    = true
      val cBuf      = new Array[Char](256)
      val sb        = new StringBuilder(256)
      loopWhile(isOpen) {
        val num = inReader.read(cBuf)
        isOpen = num >= 0
        if (isOpen) {
          sb.appendAll(cBuf, 0, num)
        } else {
          result ! sb.toString()
        }
      }
    }
  }

  private class CurlProgressPostFactory(fun: Int => Unit)
    extends PostActorFactory[Unit] {
    def make(p: Process, result: ResultActor[Unit]) = new CurlProgressPost(fun, p, result)
  }

  private class CurlProgressPost(fun: Int => Unit, p: Process, result: ResultActor[Unit])
    extends DaemonActor {
    def act {
      var isOpen = true
      var bars = 0
      val is = p.getErrorStream
      var inBars = false
      var lastBars = -1
      loopWhile(isOpen) {
        is.read() match {
          case -1 => {
            isOpen = false; result ! ()
          }
          case 13 => {
            bars = 0; inBars = true
          } // cr
          case 35 => bars += 1 // '#'
          case 32 => if (inBars) {
            // ' '
            inBars = false
            if (bars != lastBars) {
              lastBars = bars
              val perc = (bars * 100) / 72
              fun(perc)
            }
          }
          case _ =>
        }
      }
    }
  }

  private object XMLPost extends PostActorFactory[Either[Node, Throwable]] {
    def make(p: Process, result: ResultActor[Either[Node, Throwable]]) = new XMLPost(p, result)
  }

  private class XMLPost(p: Process, result: ResultActor[Either[Node, Throwable]])
    extends DaemonActor {
    def act {
      val is = p.getInputStream
      result ! (try {
        val xml = XML.load(is)
        Left(xml)
      }
      catch {
        case NonFatal(e) => Right(e)
      })
    }
  }

}