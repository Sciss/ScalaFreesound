/*
 *  TextSearch.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound

import de.sciss.serial.{DataInput, DataOutput, ConstFormat}

import scala.language.implicitConversions

object TextSearch {
  implicit def fromString(s: String): TextSearch = TextSearch(s)

  implicit object format extends ConstFormat[TextSearch] {
    private[this] val COOKIE = 0x46535453  // "FSTS" - freesound text search

    def read(in: DataInput): TextSearch = {
      import in._
      val c = readInt()
      require(c == COOKIE, s"Unexpected cookie (found ${c.toHexString}, expected ${COOKIE.toHexString})")
      val query       = readUTF()
      val filter      = Filter.format.read(in)
//      val previews    = readBoolean()
//      val images      = readBoolean()
      val sort        = Sort.format.read(in)
      val groupByPack = readBoolean()
      val maxItems    = readInt()
      TextSearch(query = query, filter = filter, /* previews = previews, images = images, */ sort = sort,
        groupByPack = groupByPack, maxItems = maxItems)
    }

    def write(v: TextSearch, out: DataOutput): Unit = {
      import v._; import out._
      writeInt(COOKIE)
      writeUTF(query)
      Filter.format.write(filter, out)
//      writeBoolean(previews)
//      writeBoolean(images  )
      Sort.format.write(sort, out)
      writeBoolean(groupByPack)
      writeInt(maxItems)
    }
  }
}
final case class TextSearch(query: String, filter: Filter = Filter(),
                            /* previews: Boolean = false, images: Boolean = false, */
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
    res                                      ::= QueryField("query", query)
    res
  }
}