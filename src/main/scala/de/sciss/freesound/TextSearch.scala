package de.sciss.freesound

import de.sciss.freesound.TextSearch.{Query, Sort}

import scala.language.implicitConversions

object TextSearch {
  object Query {
    implicit def fromString(s: String): Query = Query(s)
  }
  final case class Query(value: String) extends AnyVal

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

  implicit def fromString(s: String): TextSearch = TextSearch(s)
}
final case class TextSearch(query: Query, filter: FilterOLD = FilterOLD(), sort: Sort = Sort.Score,
                            groupByPack: Boolean = false) {

  override def toString: String = toFields.mkString("&")

  def toQueryString: String = toFields.map(_.encoded).mkString("&")

  def toFields: List[QueryField] = {
    var res = List.empty[QueryField]
    if (groupByPack)        res ::= QueryField("group_by_pack", "1")
    if (sort != Sort.Score) res ::= QueryField("sort", sort.toProperty)
    filter.toPropertyOption.foreach(f => res ::= QueryField("filter", f))
    res ::= QueryField("query", query.value)
    res
  }
}