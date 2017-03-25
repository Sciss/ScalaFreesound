package de.sciss.freesound

import scala.util.{Failure, Success}

object TestGetToken {
  def main(args: Array[String]): Unit = {
    val code = args.headOption.getOrElse(sys.error("Must specify authorization code as argument"))
    implicit val client = Freesound.readClient()
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
