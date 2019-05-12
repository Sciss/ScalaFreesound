/*
 *  SearchViewImpl.scala
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

package de.sciss.freesound.swing.impl

import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.util.concurrent.ExecutionException

import de.sciss.freesound.swing.{Collapse, FilterView, SearchView}
import de.sciss.freesound.{Client, Filter, Freesound, Sort, Sound}
import de.sciss.model.impl.ModelImpl
import de.sciss.swingplus.GroupPanel.Alignment
import de.sciss.swingplus.{GroupPanel, Separator, Spinner}
import dispatch.StatusCode
import javax.swing.{JComponent, KeyStroke, SpinnerNumberModel}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.{ExecutionContext, Future}
import scala.swing.event.{EditDone, Key}
import scala.swing.{Action, Button, Component, Label, Swing, TextField}

object SearchViewImpl {
  def apply()(implicit client: Client): SearchView = new Impl

  private final class Impl(implicit client: Client)
    extends SearchView with ModelImpl[SearchView.Update] {

    var sort            : Sort    = Sort.Score
    var groupByPack     : Boolean = false

    private[this] var _maxItemsEditable = true

    private[this] val mMaxItems = new SpinnerNumberModel(100, 1, 1000, 1)

    def maxItems: Int = mMaxItems.getNumber.intValue()
    def maxItems_=(value: Int): Unit = mMaxItems.setValue(value)

    def maxMaxItems: Int = mMaxItems.getMaximum.asInstanceOf[Number].intValue()
    def maxMaxItems_=(value: Int): Unit = mMaxItems.setMaximum(value)

    def maxItemsEditable: Boolean = _maxItemsEditable
    def maxItemsEditable_=(value: Boolean): Unit = if (_maxItemsEditable != value) {
      _maxItemsEditable   = value
      sepMaxItems.visible = value
      lbMaxItems.visible  = value
      ggMaxItems.visible  = value
    }

    private[this] var _liveMatchLag = 2000

    def liveMatchLag: Int = _liveMatchLag
    def liveMatchLag_=(value: Int): Unit = {
      _liveMatchLag = value
      coll.delay    = value
    }

    private[this] var _showLiveMatches = true

    def showLiveMatches: Boolean = _showLiveMatches
    def showLiveMatches_=(value: Boolean): Unit = {
      _showLiveMatches  = value
//      lbCount.visible   = value
//      if (!value) coll.cancel()
    }

    private[this] lazy val coll         = Collapse(liveMatchLag)

    private[this] lazy val sepMaxItems  = Separator()
    private[this] lazy val lbMaxItems   = new Label("Max items:")
    private[this] lazy val ggMaxItems   = new Spinner(mMaxItems)

    lazy val queryField: TextField = {
      val res = new TextField(10)
      res.listenTo(res)
      res.reactions += {
        case EditDone(_) => updateCount(res.text, filter)
      }
      res
    }

    val filterView: FilterView  = {
      val res = FilterView()
      res.addListener { case f =>
        updateCount(query, f)
      }
      res
    }

    private def humanReadableString(stroke: KeyStroke): String = {
      val mod = stroke.getModifiers
      val sb  = new StringBuilder
      if (mod > 0) {
        sb.append(KeyEvent.getKeyModifiersText(mod))
        sb.append('+')
      }
      sb.append(KeyEvent.getKeyText(stroke.getKeyCode))
      sb.result()
    }

    private[this] lazy val actionSearch = Action("Search") { runSearch(query, filter) }

    lazy val searchButton : Button      = {
      val menu1   = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
      val stroke  = KeyStroke.getKeyStroke(Key.Enter.id, menu1)
      val res     = new Button(actionSearch)
      val doClick = new Action(null) {
        def apply(): Unit = if (res.showing) res.doClick()
      }
      res.tooltip = humanReadableString(stroke)
      res.peer.registerKeyboardAction(doClick.peer, "freesound-search", stroke, JComponent.WHEN_IN_FOCUSED_WINDOW)
      res
    }

    private[this] lazy val lbCount = new Label()

    private[this] var futCount: Future[Int] = _

    private[this] var _querySeen  = ""
    private[this] var _filterSeen = filterView.filter

    private def updateCount(q: String, f: Filter): Unit = {
      if (_querySeen != q || _filterSeen != f) {
//        println("updateCount")
        _querySeen  = q
        _filterSeen = f
        dispatch(SearchView.FormUpdate(q, f))
        coll(runUpdateCount(q, f))
      }
    }

    private def runUpdateCount(q: String, f: Filter): Unit = {
      if (!q.isEmpty || f.nonEmpty) {
        val fut   = Freesound.textCount(query = q, filter = f)
        futCount  = fut
        import ExecutionContext.Implicits.global
        fut.onComplete { tr =>
          Swing.onEDT {
            if (futCount == fut) {
//              println("runUpdateCount done")
              lbCount.text = tr.toOption.fold("No matches")(value => s"$value matches")
              dispatch(SearchView.CountResult(q, f, tr))
            }
          }
        }
      }
    }

    private[this] var futSearch: Future[Vec[Sound]] = _

    private[this] val searchTimeout = Collapse(delay = 30 * 1000).set(actionSearch.enabled = true)

    private def runSearch(q: String, f: Filter): Unit = {
      val fut = Freesound.textSearch(query = q, filter = f, sort = sort,
        groupByPack = groupByPack, maxItems = maxItems)
      futSearch = fut
      import ExecutionContext.Implicits.global
      dispatch(SearchView.StartSearch(q, f))
      actionSearch.enabled = false
      searchTimeout.tick()
      fut.onComplete { tr =>
        Swing.onEDT {
          if (futSearch == fut) {
            // println("runSearch done")
            actionSearch.enabled = true
            searchTimeout.cancel()
            dispatch(SearchView.SearchResult(q, f, tr))

            def failure(code: Int): Unit = {
              val msg = code match {
                case 400 => "Bad request"
                case 401 => "Unauthorized"
                case 403 => "Forbidden"
                case 404 => "Not found"
                case 405 => "Method not allowed"
                case 409 => "Conflict"
                case 429 => "Too many requests"
                case x if x >= 500 && x < 600 => "Internal server error"
                case _   => ""
              }
              lbCount.text = s"Error $code $msg"
            }

            tr.failed.foreach {
              case StatusCode(c) => failure(c)
              case e: ExecutionException => e.getCause match {
                case StatusCode(c) => failure(c)
                case _ =>
              }
              case _ =>
            }
          }
        }
      }
    }

    lazy val component : Component = {
      val lbQuery = new Label("Query:")
      val glue1   = Swing.HGlue
      val strut1  = Swing.HStrut(8)

      new GroupPanel {
        horizontal = Par(
          Seq(lbQuery, queryField),
          filterView.component,
          sepMaxItems,
          Seq(lbMaxItems, ggMaxItems, glue1, lbCount, strut1, searchButton)
        )
        vertical = Seq(
          Par(Alignment.Baseline)(lbQuery, queryField),
          filterView.component,
          sepMaxItems,
          Par(Alignment.Baseline)(lbMaxItems, ggMaxItems, glue1, lbCount, strut1, searchButton)
        )
      }
    }

    def query        : String         = _querySeen
    def query_=(value: String): Unit  = {
      _querySeen      = value
      queryField.text = value
    }

    def filter        : Filter        = _filterSeen
    def filter_=(value: Filter): Unit = {
      _filterSeen       = value
      filterView.filter = value
    }
  }
}