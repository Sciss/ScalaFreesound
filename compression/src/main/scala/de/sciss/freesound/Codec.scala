/*
 *  Codec.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound

import java.io.{FileInputStream, FileOutputStream}

import de.sciss.file._
import de.sciss.jump3r
import de.sciss.audiofile.{AudioFileType, SampleFormat}
import org.jflac.metadata.StreamInfo
import org.jflac.util.{ByteData, WavWriter}
import org.jflac.{FLACDecoder, PCMProcessor}

/** Object containing various audio file type decoding or decompression routines. */
object Codec {
  def convertToWave(in: File, inType: FileType, out: File): Unit = inType match {
    case FileType.FLAC  => convertFLACToWave(in = in, out = out)
    case FileType.MP3   => convertMP3ToWave (in = in, out = out)
    case FileType.Ogg   => convertOggToWave (in = in, out = out)
    case _              => throw new UnsupportedOperationException(s"Codec conversion from $inType")
  }

  def convertMP3ToWave(in: File, out: File): Unit = {
    val lame = new jump3r.Main
    val args = Array("--silent", "--decode", "--mp3input", in.path, out.path)
    lame.run(args)
  }

  def convertFLACToWave(in: File, out: File): Unit = {
    val is = new FileInputStream(in)
    try {
      val os = new FileOutputStream(out)
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

  def convertOggToWave(in: File, out: File): Unit = {
    val fis = new FileInputStream(in)
    try {
      OggDecoder.decode(fis, out, AudioFileType.Wave, SampleFormat.Int16)
    } finally {
      fis.close()
    }
  }
}