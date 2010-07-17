/*
 *  Shell.scala
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

package de.sciss.freesound

import actors.{Actor, DaemonActor}
import java.io.{InputStream, InputStreamReader, BufferedReader}
import xml.{Node, XML}

/**
 *    @version 0.11, 17-Jul-10
 */
object Shell {
   var curlPath      = "curl"

   def command( cmd: String* )( fun: (Int, String) => Unit ) {
      new Command( cmd, StringPost, new ResultActor( fun ))
   }

   def curl( args: String* )( fun: (Int, String) => Unit ) {
      new Command( curlPath :: args.toList, StringPost, new ResultActor( fun ))
   }

   def curlXML( args: String* )( fun: (Int, Either[ Node, Throwable ]) => Unit ) {
      new Command( curlPath :: args.toList, XMLPost, new ResultActor( fun ))
   }

   def curlProgress( prog: Int => Unit, args: String* )( fun: Int => Unit ) {
      new Command( curlPath :: args.toList, new CurlProgressPostFactory( prog ),
         new ResultActor( (code: Int, _: Unit) => fun( code )))
   }

   private class Command[ T ]( cmd: Seq[ String ], post: PostActorFactory[ T ], resultActor: ResultActor[ T ]) {
//      if( verbose ) println( cmd.mkString( " " ))

      val pb         = new ProcessBuilder( cmd: _* )
      val p          = pb.start

      val postActor = post.make( p, resultActor )

      val processActor = new DaemonActor {
         def act = try {
            p.waitFor()
         } catch { case e: InterruptedException =>
            p.destroy()
         } finally {
            resultActor ! Code( p.exitValue )
         }
      }

      resultActor.start
      postActor.start
      processActor.start
   }

   private class ResultActor[ T ]( fun: (Int, T) => Unit )
   extends DaemonActor {
      def act {
         react {
            case Code( code ) => react {
               case response => {
                  fun( code, response.asInstanceOf[ T ])  // XXX hmmm... not so nice the casting...
               }
            }
         }
      }
   }

   private case class Code( value: Int )

   private trait PostActorFactory[ T ] {
      def make( p: Process, result: ResultActor[ T ]) : Actor
   }

   private object StringPost extends PostActorFactory[ String ] {
      def make( p: Process, result: ResultActor[ String ] ) = new StringPost( p, result )
   }

   private class StringPost( p: Process, result: ResultActor[ String ]) extends DaemonActor {
      def act {
         val inReader   = new BufferedReader( new InputStreamReader( p.getInputStream() ))
         var isOpen     = true
         val cBuf       = new Array[ Char ]( 256 )
         val sb         = new StringBuilder( 256 )
         loopWhile( isOpen ) {
            val num  = inReader.read( cBuf )
            isOpen   = num >= 0
            if( isOpen ) {
               sb.appendAll( cBuf, 0, num )
            } else {
               result ! sb.toString()
            }
         }
      }
   }

   private class CurlProgressPostFactory( fun: Int => Unit )
   extends PostActorFactory[ Unit ] {
      def make( p: Process, result: ResultActor[ Unit ]) = new CurlProgressPost( fun, p, result )
   }

   private class CurlProgressPost( fun: Int => Unit, p: Process, result: ResultActor[ Unit ])
   extends DaemonActor {
      def act {
         var isOpen  = true
         var bars    = 0
         val is      = p.getErrorStream()
         var inBars  = false
         var lastBars = -1
         loopWhile( isOpen ) {
            is.read() match {
               case -1 => { isOpen = false; result ! () }
               case 13 => { bars = 0; inBars = true }  // cr
               case 35 => bars += 1   // '#'
               case 32 => if( inBars ) {    // ' '
                  inBars =  false
                  if( bars != lastBars ) {
                     lastBars = bars
                     val perc = (bars * 100) / 72
                     fun( perc )
                  }
               }
               case _ =>
            }
         }
      }
   }

   private object XMLPost extends PostActorFactory[ Either[ Node, Throwable ]] {
      def make( p: Process, result: ResultActor[ Either[ Node, Throwable ]]) = new XMLPost( p, result )
   }

   private class XMLPost( p: Process, result: ResultActor[ Either[ Node, Throwable ]])
   extends DaemonActor {
      def act {
         val is = p.getInputStream()
         result ! (try {
            val xml = XML.load( is )
            Left( xml )
         }
         catch { case e => Right( e )})
      }
   }
}