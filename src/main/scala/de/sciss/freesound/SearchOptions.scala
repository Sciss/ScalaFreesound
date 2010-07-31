/*
 *  SearchOptions.scala
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

case class SearchOptions(
   keyword: String,
   descriptions : Boolean = true,
   tags : Boolean = true,
   fileNames : Boolean = false,
   userNames : Boolean = false,
   minDuration : Int = 20,
   maxDuration : Int = 1200,
   order : Int = 1,
   offset : Int = 0,
   maxItems : Int = 500
) {
   override def toString = "SearchOptions(keyword = \"" + keyword + "\", descriptions = " + descriptions +
      ", tags = " + tags + ", fileNames = " + fileNames + ", userNames = " + userNames +
      ", minDuration = " + minDuration + ", maxDuration = " + maxDuration + ", order = " + order +
      ", maxItems = " + maxItems + ")"
}