/*
 *  SearchView.scala
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

package de.sciss.freesound.swing

import de.sciss.freesound.{Client, Filter, Sort, Sound}
import de.sciss.model.Model

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{Button, Component, TextField}
import scala.util.Try

object SearchView {
  def apply()(implicit client: Client): SearchView = impl.SearchViewImpl()

  sealed trait Update
  final case class FormUpdate  (query: String, filter: Filter) extends Update
  final case class CountResult (query: String, filter: Filter, count: Try[Int]) extends Update
  final case class StartSearch (query: String, filter: Filter) extends Update
  final case class SearchResult(query: String, filter: Filter, sounds: Try[Vec[Sound]]) extends Update
}
trait SearchView extends Model[SearchView.Update] {
  def component       : Component

  var query           : String
  var filter          : Filter

  def queryField      : TextField
  def filterView      : FilterView
  def searchButton    : Button

  var showLiveMatches : Boolean
  var liveMatchLag    : Int

  var sort            : Sort
  var groupByPack     : Boolean
  var maxItems        : Int

  var maxItemsEditable: Boolean
  /** If `maxItemsEditable` is `true`, this limits the maximum value one can enter. */
  var maxMaxItems     : Int
}