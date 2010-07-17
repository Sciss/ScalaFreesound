/*
 *  SampleInfoPersistentCacheImpl.scala
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

import de.sciss.freesound.{SampleInfo, SampleInfoCache}
import java.io.{FileOutputStream, FileInputStream, IOException, File}

class SampleInfoPersistentCacheImpl( path: String )
extends SampleInfoCache {
   private val folder = new File( path )

   @throws( classOf[ IOException ])
   def init {
      if( !folder.isDirectory && !folder.mkdirs() ) {
         throw new IOException( "Could not create cache folder (" + path + ")" )
      }
   }

   def contains( id: Long ) : Boolean = file( id ).isFile()

   @throws( classOf[ IOException ])
   def get( id: Long ) : Option[ SampleInfo ] = {
      val f = file( id )
      try {
         val is = new FileInputStream( f )
         try {
            Some( SampleInfo.readXML( is ))
         }
         catch { case e =>
            None
         }
         finally {
            try { is.close } catch { case _ => }
         }
      }
      catch { case e: IOException => None }
   }

   @throws( classOf[ IOException ])
   def add( info: SampleInfo ) {
      val f    = file( info.id )
      if( f.exists ) f.delete()
      val os   = new FileOutputStream( f )
      info.writeXML( os )
      os.close()
   }

   @throws( classOf[ IOException ])
   def remove( id: Long ) {
      file( id ).delete()
   }

   private def file( id: Long ) : File =
      new File( folder, "info" + id + ".xml" )
}