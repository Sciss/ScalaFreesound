package de.sciss.freesound

import java.util.Date

import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}
import org.scalatest.FunSpec

/* To run only this test:

  testOnly de.sciss.freesound.SerializationSpec

  */
class SerializationSpec extends FunSpec {
  def trip[A](x: A)(implicit serializer: ImmutableSerializer[A]): A = {
    val out = DataOutput()
    serializer.write(x, out)
    val in  = DataInput(out.toByteArray)
    val res = serializer.read(in)
    res
  }

  describe("TextSearch") {
    it("should serialize and deserialize") {
      val ts1   = TextSearch("", filter = Filter())
      val ts1T  = trip(ts1)
      assert(ts1 === ts1T)
      assert(ts1 !== ts1T.copy(query="!"))

      import Implicits._
      val ts2   = TextSearch("foo bar", filter = Filter(
        id            = 10000 to *,
        fileName      = !"baz",
        tags          = "water" | "bucket",
        description   = !("foo" | "bar"),
        userName      = "hello",
        created       = * to new Date(),
        license       = License.CC_BY_NC_3_0,
        pack          = "pack",
        packTokens    = "pack tokens",
        geoTag        = true,
        fileType      = !FileType.MP3 & !FileType.FLAC,
        duration      = 1.0 -> 2.0,
        numChannels   = 2 to 4,
        sampleRate    = 44100,
        bitDepth      = 16 | 24,
        bitRate       = * to 1000,
        fileSize      = 10000 to *,
        numDownloads  = 1,
        avgRating     = 3.0,
        numRatings    = 5,
        comment       = "hello" & "mello",
        numComments   = * to 10,
        isRemix       = false,
        wasRemixed    = true,
        md5           = "egal"
      ), sort = Sort.CreatedNewest, groupByPack = true, maxItems = 101)
      val ts2T = trip(ts2)
      assert(ts2 === ts2T)
      assert(ts2 !== ts2T.copy(filter=ts2T.filter.copy(md5="hello")))

      val f1  = Filter(geoTag = GeoTag.Distance(GeoTag(12.34, 56.78), 9.10))
      val f1T = trip(f1)
      assert(f1 === f1T)

      val f2  = Filter(geoTag = GeoTag.Disjunction(GeoTag(12.34, 56.78), GeoTag(-1.234, -5.678)))
      val f2T = trip(f2)
      assert(f2 === f2T)

      val s1 = Sound(328168,
          fileName    = "Footsteps, Puddles, A.wav",
          tags        = List("Water", "Splosh", "Footstep"),
          description = "Raw audio of footsteps splashing in small puddles and some mud. \nPlease comment...",
          userName    = "InspectorJ",
          created     = new Date, // Sat Nov 07 02:50:56 CET 2015,
          license     = License.CC_BY_3_0,
          packId      = 18727,
          geoTag      = None,
          fileType    = FileType.Wave,
          duration    = 12.936,
          numChannels = 2,
          sampleRate  = 44100.0,
          bitDepth    = 16,
          bitRate     = 1378,
          fileSize    = 2281950,
          numDownloads= 3347,
          avgRating   = 3.8,
          numRatings  = 10,
          numComments = 8,
          userId      = 5121236
      )
      val s1T = trip(s1)
      assert(s1 === s1T)
      
      val s2 = Sound(271145,
        fileName    = "Blackberry Creek 2-15-15.aif",
        tags        = List("Cupertino", "water", "Creek", "Stream", "Water", "California", "Flowing"),
        description = "Creek at Blackberry Farm in Cupertino, California.  AIF file, untreated, recorded on iPhone on 2/15/2015.",
        userName    = "mcushman1969",
        created     = new Date, // Sun Apr 19 22:53:49 CEST 2015,
        license     = License.CC0_1_0,
        packId      = 0,
        geoTag      = Some(GeoTag(37.3197, -122.0617)),
        fileType    = FileType.AIFF,
        duration    = 20.802,
        numChannels = 2,
        sampleRate  = 44100.0,
        bitDepth    = 24,
        bitRate     = 2070,
        fileSize    = 5511952,
        numDownloads= 695,
        avgRating   = 4.8,
        numRatings  = 4,
        numComments = 2,
        userId      = 3984671
      )
      val s2T = trip(s2)
      assert(s2 === s2T)
      assert(s2 !== s2T.copy(userId = -1))
    }
  }
}