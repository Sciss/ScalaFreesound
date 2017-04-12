package de.sciss.freesound
package swing

import de.sciss.file.file
import de.sciss.submin.Submin

import scala.concurrent.{ExecutionContext, Future}
import scala.swing.{BorderPanel, Frame, Label, MainFrame, SimpleSwingApplication, Swing}

object FilterViewApp extends SimpleSwingApplication {
  override def main(args: Array[String]): Unit = {
    Submin.install(true)
    super.main(args)
  }

  implicit val client: Client = {
    val f = file("client.json")
    if (f.isFile) Freesound.readClient()
    else sys.error("Need to specify API key")
  }

  lazy val view = FilterView()

  lazy val ggCount = new Label("          ")

  private var futCount: Future[Int] = _

  def updateCount(filter: Filter): Unit = {
    println(s"New filter: $filter")
    if (filter.nonEmpty) {
      val fut = Freesound.textCount("", filter = view.filter)
      futCount = fut
      import ExecutionContext.Implicits.global
      fut.onComplete { tr =>
        Swing.onEDT {
          if (futCount == fut) {
            ggCount.text = tr.toOption.fold("No matches")(value => s"$value matches")
          }
        }
      }
    }
  }

  lazy val top: Frame = {
    val cCount = Collapse(2000)

    view.addListener {
      case filter => cCount(updateCount(filter))
    }

    new MainFrame {
      title = "Filter Test"
      contents = new BorderPanel {
        add(view.component, BorderPanel.Position.Center)
        add(ggCount, BorderPanel.Position.South)
      }
    }
  }
}
