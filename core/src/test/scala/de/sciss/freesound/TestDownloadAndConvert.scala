package de.sciss.freesound

import java.io.{FileInputStream, FileOutputStream}

import de.sciss.file._
import org.jflac.metadata.StreamInfo
import org.jflac.util.{ByteData, WavWriter}
import org.jflac.{FLACDecoder, PCMProcessor}

import scala.concurrent.blocking

object TestDownloadAndConvert {
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

    import dispatch.Defaults.executor

    val futRes = futDL.map { _ =>
      blocking {
        val is = new FileInputStream(fOut)
        try {
          val os = new FileOutputStream(fOutD)
          try {
            val d = new FLACDecoder(is)
            val w = new WavWriter  (os)
            d.addPCMProcessor(new PCMProcessor {
              def processStreamInfo(info: StreamInfo): Unit = w.writeHeader(info)
              def processPCM       (data: ByteData  ): Unit = w.writePCM   (data)
            })
            d.decode()
          } finally {
            os.close()
          }
        } finally {
          is.close()
        }
      }
    }

    futRes.onComplete { res =>
      println(s"RESULT: $res")
      if (res.isSuccess) println(s"LENGTH FLAC ${fOut.length()} WAV ${fOutD.length()}")
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}