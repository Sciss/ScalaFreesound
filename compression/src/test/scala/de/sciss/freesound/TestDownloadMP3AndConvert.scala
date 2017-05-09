package de.sciss.freesound

import de.sciss.file._
import de.sciss.jump3r

import scala.concurrent.blocking

object TestDownloadMP3AndConvert {
  def main(args: Array[String]): Unit = {
    implicit val auth: Auth = {
      val f = file("auth.json")
      if (f.isFile) Freesound.readAuth()
      else sys.error("Need to specify access token")
    }

    val soundId = 4202 // 56866
    val fOut    = file("/") / "data" / "temp" / "out.mp3"
    val fOutD   = fOut.replaceExt("wav")
    if (fOut .isFile) fOut.delete()
    if (fOutD.isFile) fOut.delete()

    val futDL = Freesound.download(id = soundId, out = fOut)

    import dispatch.Defaults.executor

    val futRes = futDL.map { _ =>
      blocking {
        Codec.convertMP3ToWave(fOut, fOutD)
      }
    }

    futRes.onComplete { res =>
      println(s"RESULT: $res")
      if (res.isSuccess) println(s"LENGTH MP3 ${fOut.length()} WAV ${fOutD.length()}")
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}