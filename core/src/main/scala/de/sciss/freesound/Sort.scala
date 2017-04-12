/*
 *  Sort.scala
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

object Sort {
  /** Sort by a relevance score returned by our search engine (default). */
  case object Score extends Sort {
    def toProperty = "score"
  }
  /** Sort by the duration of the sounds.
    *
    * @param  ascending if `true`, shortest sounds come first, if `false`, longest sounds come first
    */
  final case class Duration(ascending: Boolean) extends Sort {
    def toProperty = s"duration_${if (ascending) "asc" else "desc"}"
  }

  /** Sort by the date of when the sound was added.
    *
    * @param  ascending if `true`, oldest sounds come first, if `false`, newest sounds come first
    */
  final case class Created(ascending: Boolean) extends Sort {
    def toProperty = s"created_${if (ascending) "asc" else "desc"}"
  }

  /** Sort by the number of downloads.
    *
    * @param  ascending if `true`, least downloaded sounds come first, if `false`, most downloaded sounds come first
    */
  final case class Downloads(ascending: Boolean) extends Sort {
    def toProperty = s"downloads_${if (ascending) "asc" else "desc"}"
  }

  /** Sort by the average rating given to the sounds.
    *
    * @param  ascending if `true`, lowest rated sounds come first, if `false`, highest rated sounds come first
    */
  final case class Rating(ascending: Boolean) extends Sort {
    def toProperty = s"rating_${if (ascending) "asc" else "desc"}"
  }

  val DurationShortest: Sort = Duration  (ascending = true )
  val DurationLongest : Sort = Duration  (ascending = false)
  val CreatedOldest   : Sort = Created   (ascending = true )
  val CreatedNewest   : Sort = Created   (ascending = false)
  val DownloadsLeast  : Sort = Downloads (ascending = true )
  val DownloadsMost   : Sort = Downloads (ascending = false)
  val RatingLowest    : Sort = Rating    (ascending = true )
  val RatingHighest   : Sort = Rating    (ascending = false)
}
/** Sorting order of the search results. */
sealed trait Sort {
  def toProperty: String
}