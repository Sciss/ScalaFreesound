/*
 *  Model.scala
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

import scala.collection.immutable.{ Queue }

/**
 *    A Model implements the publish-subscribe pattern for
 *    generic (untyped) messages. Observers subscribe by
 *    calling <code>addListener</code> with an appropriate matcher
 *    function. When the Model dispatches a message, it
 *    invokes the apply method of all observers who are
 *    defined for the given message. Dispatching is
 *    synchronous, but exceptions are caught.
 *
 *    @version 0.11, 23-May-10
 */
object Model {
   type Listener = PartialFunction[ AnyRef, Unit ]
}
trait Model {
   import Model._

   private var listeners   = Queue.empty[ Listener ]
   private val sync        = new AnyRef

   protected def dispatch( change: AnyRef ) {
      listeners.foreach( l => try {
         if( l.isDefinedAt( change )) l( change )
      } catch {
         case e => e.printStackTrace() // catch, but print
      })
   }

   def addListener( l: Listener ) : Listener = {
      sync.synchronized {
         listeners = listeners.enqueue( l )
      }
      l
   }

   def removeListener( l: Listener ) : Listener = {
      sync.synchronized {
         // multi set diff just removes one instance --
         // observers could register more than once if they want
         listeners = listeners.diff( List( l ))
      }
      l
   }
}