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

import scala.concurrent.Future
import scala.util.Try
import scala.xml.Node

object Shell {
  var curlPath = "curl"

  def command(cmd: String*): Future[(Int, String)] =
    Command(cmd, StringPost)

  def curl(args: String*): Future[(Int, String)] = command(curlPath +: args: _*)

  def curlXML(args: String*)(fun: (Int, Either[Node, Throwable]) => Unit): Unit =
    Command(curlPath :: args.toList, XMLPost)

  def curlProgress(progress: Int => Unit, args: String*)(fun: Int => Unit): Future[(Int, Unit)] =
    Command(curlPath :: args.toList, new CurlProgressPostFactory(progress))

  private def Command[A](cmd: Seq[String], post: PostFactory[A]): Future[(Int, A)] = {
    val pb          = new ProcessBuilder(cmd: _*)
    val p: Process  = pb.start
    ???

//    val postActor: Any = post.make(p, resultActor)
//
//    val processActor = new DaemonActor {
//      def act = try {
//        p.waitFor()
//      } catch {
//        case e: InterruptedException =>
//          p.destroy()
//      } finally {
//        resultActor ! Code(p.exitValue)
//      }
//    }
//
//    resultActor .start
//    postActor   .start
//    processActor.start

    post.make(p)
  }

  private case class Code(value: Int)

  private trait PostFactory[A] {
    def make(p: Process): Future[(Int, A)]
  }

  private object StringPost extends PostFactory[String] {
    def make(p: Process): Future[(Int, String)] = ??? // new StringPost(p, result)
  }

//  private class StringPost(p: Process, result: ResultActor[String]) extends DaemonActor {
//    def act {
//      val inReader  = new BufferedReader(new InputStreamReader(p.getInputStream))
//      var isOpen    = true
//      val cBuf      = new Array[Char](256)
//      val sb        = new StringBuilder(256)
//      loopWhile(isOpen) {
//        val num = inReader.read(cBuf)
//        isOpen = num >= 0
//        if (isOpen) {
//          sb.appendAll(cBuf, 0, num)
//        } else {
//          result ! sb.toString()
//        }
//      }
//    }
//  }

  private class CurlProgressPostFactory(fun: Int => Unit)
    extends PostFactory[Unit] {
    def make(p: Process): Future[(Int, Unit)] = ??? // new CurlProgressPost(fun, p, result)
  }

//  private class CurlProgressPost(fun: Int => Unit, p: Process, result: ResultActor[Unit])
//    extends DaemonActor {
//    def act {
//      var isOpen = true
//      var bars = 0
//      val is = p.getErrorStream
//      var inBars = false
//      var lastBars = -1
//      loopWhile(isOpen) {
//        is.read() match {
//          case -1 =>
//            isOpen = false
//            result ! ()
//          case 13 =>
//            bars = 0
//            inBars = true // cr
//          case 35 => bars += 1 // '#'
//          case 32 => if (inBars) {
//            // ' '
//            inBars = false
//            if (bars != lastBars) {
//              lastBars = bars
//              val perc = (bars * 100) / 72
//              fun(perc)
//            }
//          }
//          case _ =>
//        }
//      }
//    }
//  }

  private object XMLPost extends PostFactory[Try[Node]] {
    def make(p: Process): Future[(Int, Try[Node])] = ??? // new XMLPost(p, result)
  }

//  private class XMLPost(p: Process, result: ResultActor[Either[Node, Throwable]])
//    extends DaemonActor {
//    def act {
//      val is = p.getInputStream
//      result ! (try {
//        val xml = XML.load(is)
//        Left(xml)
//      }
//      catch {
//        case NonFatal(e) => Right(e)
//      })
//    }
//  }

}