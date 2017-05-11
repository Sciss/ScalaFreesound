package de.sciss.freesound

import java.io.{IOException, InputStream}

import com.jcraft.jogg.{Packet, Page, StreamState, SyncState}
import com.jcraft.jorbis.{Block, Comment, DspState, Info}
import de.sciss.file.File
import de.sciss.synth.io.{AudioFile, AudioFileSpec, AudioFileType, SampleFormat}

/** This is essentially the `DecodeExample.java` from JOrbis, translated to Scala
  * and adapted to work with ScalaAudioFile.
  *
  * Copyright (C) 2000 ymnk, JCraft,Inc. License: LGPL v2+
  * Copyright (C) 2017 Hanns Holger Rutz.
  */
object OggDecoder {
  /** Decodes an Ogg Vorbis input stream to a PCM output file.
    *
    * @param is       the stream to decode
    * @param fOut     the output file
    * @param tpeOut   the audio file type for the output file
    * @param fmtOut   the sample format for the output file
    */
  def decode(is: InputStream, fOut: File, tpeOut: AudioFileType = AudioFileType.Wave,
             fmtOut: SampleFormat = SampleFormat.Int16): AudioFileSpec = {
    val sync    = new SyncState   // sync and verify incoming physical bit stream
    val stream  = new StreamState // take physical pages, weld into a logical stream of packets
    val page    = new Page        // one Ogg bit stream page. Vorbis packets are inside
    val packet  = new Packet      // one raw packet of data for decode
    val info    = new Info        // structure that stores all the static Vorbis bit stream settings
    val comment = new Comment     // structure that stores all the bit stream user comments
    val dsp     = new DspState    // central working state for the packet->PCM decoder
    val block   = new Block(dsp)  // local working space for packet->PCM decode

    sync.init()

    var afOut: AudioFile = null
    val _pcm    = new Array[Array[Array[Float]]](1)

    try {
      while ({ // we repeat if the bit stream is chained

        // grab some data at the head of the stream.  We want the first page
        // (which is guaranteed to be small and only contain the Vorbis
        // stream initial header) We need the first page to get the stream
        // serial no.

        // submit a 4k block to libvorbis' Ogg layer
        val off = sync.buffer(4096)
        val buf = sync.data
        val len = is.read(buf, off, 4096)
        sync.wrote(len)

        // Get the first page.
        val hasPage = sync.pageout(page) == 1
        if (!hasPage && len == 4096) throw new IOException(s"Input does not appear to be an Ogg bit stream")
        hasPage

      }) {
        var eos = false

        // Get the serial number and set up the rest of decode.
        // serial no first; use it to set up a logical stream
        stream.init(page.serialno())

        // extract the initial header from the first page and verify that the
        // Ogg bit stream is in fact Vorbis data

        // I handle the initial header first instead of just having the code
        // read all three Vorbis headers at once because reading the initial
        // header is an easy way to identify a Vorbis bit stream and it's
        // useful to see that functionality separated out.

        info   .init()
        comment.init()

        if (stream.pagein(page) < 0) {
          // error; stream version mismatch perhaps
          throw new IOException(s"Error reading first page of Ogg bit stream data")
        }

        if (stream.packetout(packet) != 1) {
          // no page? must be not Vorbis
          throw new IOException(s"Error reading initial header packet")
        }

        if (info.synthesis_headerin(comment, packet) < 0) {
          // error case; not a Vorbis header
          throw new IOException(s"The Ogg bit stream does not contain Vorbis audio data")
        }

        // At this point, we're sure we're Vorbis.  We've set up the logical
        // (Ogg) bit stream decoder.  Get the comment and code-book headers and
        // set up the Vorbis decoder

        // The next two packets in order are the comment and code-book headers.
        // They're likely large and may span multiple pages.  Thus we read
        // and submit data until we get our two packets, watching that no
        // pages are missing.  If a page is missing, error out; losing a
        // header page is the only place where missing data is fatal.

        var i = 0
        while (i < 2) {
          var result = 0
          do {
            result = sync.pageout(page)
            // if(result==0) break; // Need more data

            // Don't complain about missing or corrupt data yet.  We'll
            // catch it at the packet output phase

            if (result == 1) {
              stream.pagein(page) // we can ignore any errors here
              // as they'll also become apparent
              // at packet-out
              do {
                result = stream.packetout(packet)
                // if(result==0) break;
                if (result == -1) {
                  // Uh oh; data at some point was corrupted or missing!
                  // We can't tolerate that in a header.  Die.
                  throw new IOException(s"Corrupt secondary header")
                }

                if (result == 1) {
                  info.synthesis_headerin(comment, packet)
                  i += 1
                }
              } while (i < 2 && result != 0)
            }
          } while (i < 2 && result != 0)

          // no harm in not checking before adding more
          val off = sync.buffer(4096)
          val buf = sync.data
          val len = is.read(buf, off, 4096)

          if(len == 0 && i < 2) {
            throw new IOException(s"End of file before finding all Vorbis headers!")
          }
          sync.wrote(len)
        }

        // Throw the comments plus a few lines about the bitstream we're
        // decoding
        // ... not!

        if (afOut == null) {
          afOut = AudioFile.openWrite(fOut,
            AudioFileSpec(tpeOut, fmtOut, numChannels = info.channels, sampleRate = info.rate))
        } else {
          if (afOut.numChannels != info.channels)
            throw new IOException(s"Cannot chain bit streams of varying number of channels (${afOut.numChannels} -> ${info.channels})")
        }

        val convSize = 4096 // / info.channels

        // OK, got and parsed all three headers. Initialize the Vorbis
        //  packet->PCM decoder.
        dsp.synthesis_init(info)  // central decode state
        block.init(dsp)           // local state for most of the decode

        // so multiple block decodes can
        // proceed in parallel.  We could init
        // multiple vorbis_block structures
        // for vd here

        val _index = new Array[Int](info.channels)
        // The rest is just a straight decode loop until end of stream
        while (!eos) {
          var result0 = 0
          do {
            result0 = sync.pageout(page)
            // if(result==0) break; // need more data
            if (result0 == -1) { // missing or corrupt data at this page position
              System.err.println("Corrupt or missing data in bit stream; continuing...")
            }
            if (result0 == 1) {
              stream.pagein(page) // can safely ignore errors at
              // this point
              var result1 = 0
              do {
                result1 = stream.packetout(packet)

                //                if(result1==0) break; // need more data
                if (result1 == -1) { // missing or corrupt data at this page position
                  // no reason to complain; already complained above
                }
                if (result1 == 1) {
                  // we have a packet.  Decode it
                  if (block.synthesis(packet) == 0) { // test for success!
                    dsp.synthesis_blockin(block)
                  }

                  // **pcm is a multichannel float vector.  In stereo, for
                  // example, pcm[0] is left, and pcm[1] is right.  samples is
                  // the size of each channel.  Convert the float values
                  // (-1.<=range<=1.) to whatever PCM format and write it out

                  var avail = 0
                  do {
                    avail = dsp.synthesis_pcmout(_pcm, _index)
                    if (avail > 0) {
                      val chunk = math.min(avail, convSize)
                      val off   = _index(0)
                      val buf   = _pcm(0)
                      afOut.write(buf, off, chunk)

                      // tell libvorbis how
                      // many samples we
                      // actually consumed
                      dsp.synthesis_read(chunk)
                    }
                  } while (avail > 0)
                }
              } while (result1 != 0)

              if (page.eos() != 0) eos = true
            }
          } while (!eos && result0 != 0)

          if (!eos) {
            val off = sync.buffer(4096)
            val buf = sync.data
            val len = is.read(buf, off, 4096)
            sync.wrote(len)
            if (len == 0) eos = true
          }
        }

        // clean up this logical bit stream; before exit we see if we're
        // followed by another [chained]

        stream.clear()

        // ogg_page and ogg_packet structures always point to storage in
        // libvorbis.  They're never freed or manipulated directly

        block.clear()
        dsp  .clear()
        info .clear()  // must be called last
      }

      // OK, clean up the framer
      sync.clear()

      afOut.spec.copy(numFrames = afOut.numFrames)

    } finally {
      if (afOut != null) afOut.close()
    }
  }
}