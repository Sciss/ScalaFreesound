package de.sciss.freesound

import java.io.FileInputStream

import de.sciss.file._
import de.sciss.synth.io.{AudioFileType, SampleFormat}

object TestOggDecoding {
  def main(args: Array[String]): Unit = run()

  def run(): Unit = {
    val baseDir = file("/") / "data" / "temp"
    val fIn     = baseDir / "freesound_cache" / "330024_1196472-lq.ogg"
    val fOut    = baseDir / "330024_1196472-lq.wav"
    val fis     = new FileInputStream(fIn)
    try {
      OggDecoder.decode(fis, fOut, AudioFileType.Wave, SampleFormat.Int16)
    } finally {
      fis.close()
    }
  }
}