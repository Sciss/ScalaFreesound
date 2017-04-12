package de.sciss.freesound

import de.sciss.file._

object TestDownload {
  def main(args: Array[String]): Unit = {
    implicit val auth: Auth = {
      val f = file("auth.json")
      if (f.isFile) Freesound.readAuth()
      else sys.error("Need to specify access token")
    }

    val soundId = 31362
    val fOut    = file("/") / "data" / "temp" / "out.wav"
    if (fOut.isFile) fOut.delete()

    val futRes = Freesound.download(id = soundId, out = fOut)

    import dispatch.Defaults.executor

    futRes.onComplete { res =>
      println(s"RESULT: $res")
      if (res.isSuccess) println(s"LENGTH ${fOut.length()}")
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}