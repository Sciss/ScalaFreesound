/*
 *  Lightweight.scala
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

package de.sciss.freesound.impl

import java.util.Date

import de.sciss.freesound.{Comment, Description, Statistics, User}

case class UserImpl(id: Long)(val name: String) extends User {
  override def toString = s"User($id, $name)"
}

case class DescriptionImpl(user: User, text: String) extends Description {
  override def toString = s"Description($user, $text)"
}

case class CommentImpl(user: User, date: Date, text: String) extends Comment {
  override def toString = s"Comment($user, $date, $text)"
}

case class StatisticsImpl(numDownloads: Int, numRatings: Int, rating: Int) extends Statistics {
  override def toString = s"Statistics(numDownloads = $numDownloads, rating = $rating)"
}
