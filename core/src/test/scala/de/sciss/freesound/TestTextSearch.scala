package de.sciss.freesound

import de.sciss.file._
import de.sciss.freesound.impl.FreesoundImpl

import scala.util.{Failure, Success}

object TestTextSearch {
  def main(args: Array[String]): Unit = {
    FreesoundImpl.DEBUG = true

    implicit val client: Client = {
      val f = file("client.json")
      if (f.isFile) Freesound.readClient()
      else sys.error("Need to specify API key")
    }
//    val fut   = Freesound.textSearch("fish", Filter(duration = 4 to 100, tags = "portugal"))
    val fut = Freesound.textSearch("water", Filter(numChannels = 2, sampleRate = 44100), maxItems = 10)

    import scala.concurrent.ExecutionContext.Implicits.global

    // prevent JVM from instantly exiting
    new Thread { override def run(): Unit = this.synchronized(this.wait()) }.start()

    fut.onComplete {
      case Success(xs) =>
        println(s"Search found ${xs.size} sounds.")
        xs.foreach { snd =>
          println(snd)
          // val err = Try(new URI(snd.license)).isFailure
          // if (err) println(s"Cannot parse license URL '${snd.license}'")
          // if (snd.pack.isDefined) println(s"YES PACK ${snd.pack.get}")
          // if (snd.geoTag.isDefined) println(s"YES GEO ${snd.geoTag.get}")

          require(snd.fileName     != null)
          require(snd.tags         != null)
          require(snd.description  != null)
          require(snd.userName     != null)
        }
        sys.exit()
      case Failure(ex) =>
        println("The search failed:")
        ex.printStackTrace()
        sys.exit(1)
    }
  }
}
