package de.sciss.freesound

import de.sciss.file._

import scala.util.{Failure, Success}

object TestTextCount {
  def main(args: Array[String]): Unit = {
    implicit val client: Client = {
      val f = file("client.json")
      if (f.isFile) Freesound.readClient()
      else sys.error("Need to specify API key")
    }
    val fut = Freesound.textCount("water", Filter(numChannels = 2, sampleRate = 44100))

    import dispatch.Defaults.executor

    fut.onComplete {
      case Success(count) =>
        println(s"Search found $count matches.")
        sys.exit()
      case Failure(ex) =>
        println("The search failed:")
        ex.printStackTrace()
        sys.exit(1)
    }
  }
}
