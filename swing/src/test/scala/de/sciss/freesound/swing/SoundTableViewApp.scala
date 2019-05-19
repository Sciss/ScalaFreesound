package de.sciss.freesound
package swing

import de.sciss.file._
import de.sciss.submin.Submin

import scala.swing.{BorderPanel, Frame, MainFrame, SimpleSwingApplication, Swing}
import scala.util.{Failure, Success}

object SoundTableViewApp extends SimpleSwingApplication {
  lazy val view = SoundTableView()

  override def main(args: Array[String]): Unit = {
    Submin.install(true)
    super.main(args)

    implicit val client: Client = {
      val f = file("client.json")
      if (f.isFile) Freesound.readClient()
      else sys.error("Need to specify API key")
    }

    val fut = Freesound.textSearch("rising", Filter(numChannels = 2, sampleRate = 44100), maxItems = 48 /* 24 */)

    import scala.concurrent.ExecutionContext.Implicits.global

    fut.onComplete {
      case Success(xs) =>
        println(s"Search found ${xs.size} sounds.")
        Swing.onEDT {
          view.sounds = xs
        }

      case Failure(ex) =>
        println("The search failed:")
        ex.printStackTrace()
        sys.exit(1)
    }
  }

  lazy val top: Frame = {
    new MainFrame {
      title = "Sound Table View Test"
      contents = new BorderPanel {
        add(view.component, BorderPanel.Position.Center)
        // add(ggCount, BorderPanel.Position.South)
      }
    }
  }
}
