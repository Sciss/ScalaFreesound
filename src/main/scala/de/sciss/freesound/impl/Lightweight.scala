/*
 *  Lightweight.scala
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

import java.util.Date
import de.sciss.freesound.{Statistics, Comment, Description, User}

case class UserImpl( id: Long )( val name: String ) extends User {
   override def toString = "User(" + id + ", " + name + ")"
}

case class DescriptionImpl( user: User, text: String ) extends Description {
   override def toString = "Description(" + user + ", " + text + ")"
}

case class CommentImpl( user: User, date: Date, text: String ) extends Comment {
   override def toString = "Comment(" + user + ", " + date + ", " + text + ")"
}

case class StatisticsImpl( numDownloads: Int, numRatings: Int, rating: Int ) extends Statistics {
   override def toString = "Statistics(numDownloads = " + numDownloads + ", rating = " + rating + ")"
}
