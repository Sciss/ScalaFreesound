package de.sciss.freesound

import scala.util.{Failure, Success}

object TestGetToken {
  def main(args: Array[String]): Unit = {
    implicit val client: Client = Freesound.readClient()
    val code = args.headOption.getOrElse(sys.error(
      s"Must specify authorization code as argument! Get a refresh one: ${Freesound.urlWebAuthorize.format(client.id)}"))

    // prevent JVM from instantly exiting
    new Thread { override def run(): Unit = this.synchronized(this.wait()) }.start()

    import dispatch.Defaults.executor
    Freesound.getAuth(code).onComplete {
      case Success(auth) =>
        println("Writing...")
        Freesound.writeAuth()(auth)
        sys.exit()
      case Failure(ex) =>
        println("getAuth failed:")
        ex.printStackTrace()
        sys.exit(1)
    }
  }
}
