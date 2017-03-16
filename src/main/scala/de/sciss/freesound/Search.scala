/*
 *  Search.scala
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

import de.sciss.model.Model

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.Future

object Search {

  sealed trait Update
  case object SearchBegin extends Update

  sealed abstract class SearchResult extends Update

  case class SearchDone(ids: Vec[Sample])     extends SearchResult
  sealed abstract class SearchFailed          extends SearchResult
  case object SearchFailedCurl                extends SearchFailed
  case object SearchFailedTimeout             extends SearchFailed
  case class SearchFailedParse(e: Throwable)  extends SearchFailed

}

trait Search extends Model[Search.Update] {

  import Search._

  def perform(): Unit

  def options: SearchOptions

  def samples: Option[Vec[Sample]] = result.flatMap(_ match {
    case SearchDone(smps) => Some(smps)
    case _ => None
  })

  def result: Option[SearchResult]

  def queryResult: Future[SearchResult]

  val login: Login
}