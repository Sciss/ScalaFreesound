package de.sciss.freesound

import de.sciss.file._

object TestDownload {
  def main(args: Array[String]): Unit = {
    implicit val accessToken: AccessToken = args.headOption.getOrElse(sys.error("Must specify access token"))

    val soundId = 31362
    val fOut    = file("/") / "data" / "temp" / "out.wav"
    if (fOut.isFile) fOut.delete()

//    import dispatch._
//    import Defaults._
//    val req0    = url(Freesound.urlSoundDownload.format(soundId))
//    val req     = req0.addHeader("Authorization", s"Bearer $accessToken")
//    val handler = FileWithProgress(fOut) { (pos, size) =>
//      val p = (pos * 100) / size
//      println(s"progress: $p% ($pos of $size)")
//    }
//    val futRes  = Http(req > handler)

    val futRes = Freesound.download(id = soundId, out = fOut)

    import dispatch.Defaults.executor

    futRes.onComplete { res =>
      println(s"RESULT: $res")
      if (res.isSuccess) println(s"LENGTH ${fOut.length()}")
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}
