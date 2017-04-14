package de.sciss.freesound
package lucre

import java.net.URI

import de.sciss.file._
import de.sciss.filecache
import de.sciss.filecache.{MutableConsumer, MutableProducer}
import de.sciss.freesound.Implicits._
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}
import scala.util.{Failure, Success}

object RetrievalApp extends SimpleSwingApplication {
  implicit val client: Client = Freesound.readClient()

  implicit object uriSerializer extends ImmutableSerializer[URI] {
    def read (in         : DataInput ): URI  = new URI(in.readUTF())
    def write(v: URI, out: DataOutput): Unit = out.writeUTF(v.toString)
  }

  implicit val previewCache: MutableConsumer[URI, File] = {
    val config = filecache.Config[URI, File]()
    config.capacity = filecache.Limit(count = 100, space = 4L << 1024 << 1024)
    config.evict    = (_ /* uri */, f) => if (!f.delete()) f.deleteOnExit()
    config.space    = (_ /* uri */, f) => f.length()
    config.accept   = (_ /* uri */, f) => f.length() > 0L
    val baseDir     = file("/") / "data" / "temp"
    require(baseDir.isDirectory)
    config.folder   = baseDir / "freesound_cache"
    config.folder.mkdirs()

    val prod: MutableProducer[URI, File] = MutableProducer(config)
    MutableConsumer(prod) { uri =>
      val uriS    = uri.getPath
      val name    = uriS.substring(uriS.lastIndexOf('/') + 1)
      val out     = config.folder / name
      val proc    = Freesound.downloadPreview(uri, out = out)
      implicit val exec = config.executionContext
      proc.transform {
        case Success(())  => Success(out)
        case Failure(e)   => config.evict(uri, out); Failure(e)
      }
    }
  }

  lazy val top: Frame = {
    val queryInit   = "water"
    val filterInit  = Filter(numChannels = 2, sampleRate = 44100 to *, duration = 10.0 to *)
    val view        = FreesoundRetrievalView(queryInit = queryInit, filterInit = filterInit)
    new MainFrame {
      contents = view.component
    }
  }
}