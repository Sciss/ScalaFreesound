/*
 *  Sample.scala
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

import actors.Future

/**
 *    @version 0.10, 15-Jul-10
 */
object Sample {
   case object InfoBegin
   case class InfoDone( i: SampleInfo )
   sealed abstract class InfoFailed
   case object InfoFailedCurl extends InfoFailed
   case object InfoFailedTimeout extends InfoFailed
   case class InfoFailedParse( e: Throwable ) extends InfoFailed
   case object InfoFlushed
}

trait Sample extends Model {
   def id : Long
   def info : Option[ SampleInfo ]
   def flushInfo : Unit
   def queryInfo : Future[ Option[ SampleInfo ]]
}