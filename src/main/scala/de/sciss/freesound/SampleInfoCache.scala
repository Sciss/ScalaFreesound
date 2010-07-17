/*
 *  SampleInfoCache.scala
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

import impl.SampleInfoPersistentCacheImpl
import java.io.IOException

/**
 *    @version 0.10, 17-Jul-10
 */
object SampleInfoCache {
//   def memory : SampleInfoCache = new SampleInfoMemCacheImpl
   @throws( classOf[ IOException ])
   def persistent( path: String ) : SampleInfoCache = {
      val res = new SampleInfoPersistentCacheImpl( path )
      res.init
      res
   }
}

trait SampleInfoCache {
   def contains( id: Long ) : Boolean

   @throws( classOf[ IOException ])
   def get( id: Long ) : Option[ SampleInfo ]

   @throws( classOf[ IOException ])
   def add( info: SampleInfo ) : Unit

   @throws( classOf[ IOException ])
   def remove( id: Long ) : Unit
}