package de.sciss.freesound

import de.sciss.file._

object TestDownload {
  def main(args: Array[String]): Unit = {
    val accessToken = args.headOption.getOrElse(sys.error("Must specify access token"))
    import dispatch._, Defaults._
    val soundId = 31362
    val req0    = url(Freesound.urlSoundDownload.format(soundId))
    val req     = req0.addHeader("Authorization", s"Bearer $accessToken")
    val fOut    = file("/") / "data" / "temp" / "out.wav"

    val futRes  = Http(req > as.File(fOut))
    futRes.onComplete { res =>
      println(s"RESULT: $res")
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}
