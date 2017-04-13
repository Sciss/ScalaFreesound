/*
 *  TextSearch.scala
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

import de.sciss.freesound.{Filter, QueryField, Sort}

import scala.language.implicitConversions

object TextSearch {
//  object Query {
//    implicit def fromString(s: String): Query = Query(s)
//  }
//  final case class Query(value: String) extends AnyVal

  implicit def fromString(s: String): TextSearch = TextSearch(s)
}
final case class TextSearch(query: String, filter: Filter = Filter(), previews: Boolean = false,
                            sort: Sort = Sort.Score,
                            groupByPack: Boolean = false, maxItems: Int = 100) {
  require(maxItems >= 0)

  override def toString: String = toFields.mkString("&")

  def toQueryString: String = toFields.map(_.encoded).mkString("&")

  def toFields: List[QueryField] = {
    var res = List.empty[QueryField]
    if (groupByPack)                     res ::= QueryField("group_by_pack", "1")
    if (sort != Sort.Score)              res ::= QueryField("sort", sort.toProperty)
    filter.toPropertyOption.foreach(f => res ::= QueryField("filter", f))
    res ::= QueryField("query", query)
    res
  }
}