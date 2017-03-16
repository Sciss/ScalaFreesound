/*
 *  SearchOptions.scala
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

case class SearchOptions(
                          keyword: String,
                          descriptions: Boolean = true,
                          tags: Boolean = true,
                          fileNames: Boolean = false,
                          userNames: Boolean = false,
                          minDuration: Int = 20,
                          maxDuration: Int = 1200,
                          order: Int = 1,
                          offset: Int = 0,
                          maxItems: Int = 500
                        ) {
  override def toString: String = "SearchOptions(keyword = \"" + keyword + "\", descriptions = " + descriptions +
    ", tags = " + tags + ", fileNames = " + fileNames + ", userNames = " + userNames +
    ", minDuration = " + minDuration + ", maxDuration = " + maxDuration + ", order = " + order +
    ", maxItems = " + maxItems + ")"
}