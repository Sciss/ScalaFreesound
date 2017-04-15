package de.sciss.freesound

import java.util.Date

import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}
import org.scalatest.FunSpec

/* To run only this test:

  test-only de.sciss.freesound.SerializationSpec

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
        id = 10000 to *, fileName = !"baz", tags = "water" | "bucket",
        description = !("foo" | "bar"), userName = "hello",
        created = * to new Date(), license = License.CC_BY_NC_3_0,
        pack = "pack", packTokens = "pack tokens", geoTag = true,
        fileType = !FileType.MP3 & !FileType.FLAC, duration = 1.0 to 2.0,
        numChannels = 2 to 4, sampleRate = 44100, bitDepth = 16 | 24,
        bitRate = * to 1000, fileSize = 10000 to *, numDownloads = 1,
        avgRating = 3.0, numRatings = 5, comment = "hello" & "mello",
        numComments = * to 10, isRemix = false, wasRemixed = true,
        md5 = "egal"
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
    }
  }
}