package de.sciss.freesound

import de.sciss.file._

import scala.concurrent.blocking

object TestDownloadFLACAndConvert {
  def main(args: Array[String]): Unit = {
    implicit val auth: Auth = {
      val f = file("auth.json")
      if (f.isFile) Freesound.readAuth()
      else sys.error("Need to specify access token")
    }

    val soundId = 221752 // 360594
    val fOut    = file("/") / "data" / "temp" / "out.flac"
    val fOutD   = fOut.replaceExt("wav")
    if (fOut .isFile) fOut.delete()
    if (fOutD.isFile) fOut.delete()

    val futDL = Freesound.download(id = soundId, out = fOut)

    import scala.concurrent.ExecutionContext.Implicits.global

    val futRes = futDL.map { _ =>
      blocking {
        Codec.convertFLACToWave(fOut, fOutD)
      }
    }

    futRes.onComplete { res =>
      println(s"RESULT: $res")
      if (res.isSuccess) println(s"LENGTH FLAC ${fOut.length()} WAV ${fOutD.length()}")
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}