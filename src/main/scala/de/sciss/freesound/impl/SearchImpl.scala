///*
// *  SearchImpl.scala
// *  (ScalaFreesound)
// *
// *  Copyright (c) 2010-2017 Hanns Holger Rutz. All rights reserved.
// *
// *	This software is published under the GNU Lesser General Public License v2.1+
// *
// *
// *  For further information, please contact Hanns Holger Rutz at
// *  contact@sciss.de
// */
//
//package de.sciss.freesound
//package impl
//
//import scala.collection.breakOut
//import scala.collection.immutable.{IndexedSeq => Vec}
//import scala.concurrent.Future
//import scala.util.control.NonFatal
//
//object SearchImpl {
//
//  private case class ISearchDone(ids: Vec[Long])
//
//}
//
//class SearchImpl(val options: SearchOptions, val login: LoginImpl)
//  extends DaemonActor with Search {
//
//  import Freesound.{searchURL, verbose}
//  import Impl._
//  import Search._
//  import SearchImpl._
//
//  @volatile var result: Option[SearchResult] = None
//
//  private lazy val searchActor: Actor = {
//    start
//    this
//  }
//
//  override def toString = s"Search($options)"
//
//  // we can't use start as name because that
//  // returns an actor... which is opaque in
//  // the implementation
//  def perform(): Unit = searchActor ! IPerform
//
//  def queryResult: Future[SearchResult] = searchActor !! (IGetResult, {
//    case r: SearchResult => r
//  })
//
//  private def loopResult(res: SearchResult): Unit = {
//    result = Some(res)
//    dispatch(res)
//    loop {
//      react { case _ => reply(res) }
//    }
//  }
//
//  def act {
//    react { case IPerform =>
//      execSearch
//      if (verbose) println("Performing search...")
//      dispatch(SearchBegin)
//      react {
//        case ITimeout =>
//          if (verbose) err("Timeout while performing search.")
//          loopResult(SearchFailedTimeout)
//        case IFailed(code) =>
//          if (verbose) err(s"There was an error during the search ($code).")
//          loopResult(SearchFailedCurl)
//        case IException(cause) =>
//          if (verbose) {
//            val m = s"${cause.getClass.getName} : ${cause.getMessage}"
//            err(s"The search results could not be parsed ($m).")
//          }
//          loopResult(SearchFailedParse(cause))
//        case ISearchDone(ids) =>
//          if (verbose) {
//            val sz = ids.size
//            println(s"Search was successful ($sz sample${if (sz < 2) "" else "s"} found).")
//          }
//          val samples = ids.map(Sample(_))
//          loopResult(SearchDone(samples))
//      }
//    }
//  }
//
//  private def execSearch(): Unit = {
//    Shell.curlXML("-b", login.cookiePath, "-d", "search=" + options.keyword +
//      "&start=" + options.offset + "&searchDescriptions=" + (if (options.descriptions) 1 else 0) +
//      "&searchTags=" + (if (options.tags) 1 else 0) + "&searchFilenames=" + (if (options.fileNames) 1 else 0) +
//      "&searchUsernames=" + (if (options.userNames) 1 else 0) + "&durationMin=" + options.minDuration +
//      "&durationMax=" + options.maxDuration + "&order=" + options.order + "&limit=" + options.maxItems,
//      searchURL) { (code, response) =>
//
//      if (code != 0) {
//        searchActor ! IFailed(code)
//      } else response match {
//        case Left(xml) => try {
//          val elems = xml \ "sample"
//          val ids: Vec[Long] = elems.map(e => (e \ "@id").text.toLong)(breakOut)
//          searchActor ! ISearchDone(ids)
//        }
//        catch {
//          case NonFatal(e) =>
//            searchActor ! IException(e)
//        }
//
//        case Right(e) => searchActor ! IException(e)
//      }
//    }
//  }
//}