package de.sciss.freesound
package swing

import de.sciss.submin.Submin

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}
import scala.swing.Swing._

object SearchViewApp extends SimpleSwingApplication {
  override def main(args: Array[String]): Unit = {
    Submin.install(true)
    super.main(args)
  }

  implicit val client: Client = Freesound.readClient()

  lazy val viewSearch   = SearchView()
  lazy val viewResults  = SoundTableView()

  lazy val top: Frame = {
    viewSearch.addListener {
      case _: SearchView.FormUpdate   => println("Form update.")
      case _: SearchView.StartSearch  => println("Search start...")
      case r: SearchView.SearchResult =>
        println(s"Search ${if (r.sounds.isSuccess) "succeeded." else "failed!"}")
        if (r.sounds.isFailure) r.sounds.failed.get.printStackTrace()
        r.sounds.foreach(sounds =>
          viewResults.sounds = sounds
        )
    }

    val fSearch = new MainFrame {
      title     = "Search"
      contents  = viewSearch.component
      pack().open()
    }

    /* val fResults = */ new MainFrame {
      title     = "Results"
      contents  = viewResults.component
      pack()
      location  = {
        val b = fSearch.bounds
        (b.x + b.width + 10, 0)
      }
      open()
    }

    fSearch
  }
}