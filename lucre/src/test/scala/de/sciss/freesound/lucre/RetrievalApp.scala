package de.sciss.freesound
package lucre

import de.sciss.file._
import de.sciss.freesound.Implicits._
import de.sciss.lucre.swing.deferTx
import de.sciss.lucre.synth.InMemory
import de.sciss.submin.Submin
import de.sciss.synth.proc.AuralSystem

import scala.swing.MainFrame

object RetrievalApp {
  implicit val client: Client = Freesound.readClient()

  def main(args: Array[String]): Unit = run()

  type S = InMemory

  def run(): Unit = {
    Submin.install(true)

    val baseDir   = file("/") / "data" / "temp"
    require(baseDir.isDirectory)
    val cacheDir  = baseDir / "freesound_cache"
    cacheDir.mkdirs()

    implicit val auralSystem: AuralSystem   = AuralSystem()
    implicit val system     : S             = InMemory()

    system.step { implicit tx =>
      auralSystem.start()

      implicit val cache: PreviewsCache = PreviewsCache(dir = cacheDir)

      val queryInit   = "water"
      val filterInit  = Filter(numChannels = 2, sampleRate = 44100 to *, duration = 10.0 to *)
      val view        = FreesoundRetrievalView[S](queryInit = queryInit, filterInit = filterInit)

      deferTx(guiInit(view))
    }
  }

  def guiInit(view: FreesoundRetrievalView[S]): Unit = {
    val top = new MainFrame {
      title     = "Retrieval Test"
      contents  = view.component
    }
    top.pack().centerOnScreen()
    top.open()
  }
}