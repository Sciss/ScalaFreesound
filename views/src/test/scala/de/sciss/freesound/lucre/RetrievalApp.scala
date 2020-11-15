package de.sciss.freesound
package lucre

import de.sciss.file._
import de.sciss.freesound.Implicits._
import de.sciss.freesound.impl.FreesoundImpl
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.synth.InMemory
import de.sciss.submin.Submin
import de.sciss.proc.Universe

import scala.swing.MainFrame

object RetrievalApp {
  implicit val client: Client = Freesound.readClient()

  def main(args: Array[String]): Unit = run()

  type S = InMemory
  type T = InMemory.Txn

  def run(): Unit = {
    Submin.install(true)
    FreesoundImpl.DEBUG = true

    val baseDir   = file("/") / "data" / "temp"
    require(baseDir.isDirectory)
    val cacheDir  = baseDir / "freesound_cache"
    cacheDir.mkdirs()

    implicit val system: S = InMemory()

    system.step { implicit tx =>
      implicit val universe: Universe[T] = Universe.dummy
      universe.auralSystem.start()

      implicit val cache: PreviewsCache = PreviewsCache(dir = cacheDir)

      val queryInit   = "water"
      val filterInit  = Filter(numChannels = 2, sampleRate = 44100 to *, duration = 10.0 to *)
      val tsInit      = TextSearch(queryInit, filterInit)
      val view        = RetrievalView[T](tsInit)

      deferTx(guiInit(view))
    }
  }

  def guiInit(view: RetrievalView[T]): Unit = {
    val top = new MainFrame {
      title     = "Retrieval Test"
      contents  = view.component
    }
    top.pack().centerOnScreen()
    top.open()
  }
}
