package de.sciss.freesound

import de.sciss.file.file

import scala.util.{Failure, Success}

object TestRefreshAuth {
  def main(args: Array[String]): Unit = {
    implicit val client  = Freesound.readClient()
    implicit val auth: Auth = {
      val f = file("auth.json")
      if (f.isFile) Freesound.readAuth()
      else sys.error("Need to specify access token")
    }

    import dispatch.Defaults.executor
    Freesound.refreshAuth().onComplete {
      case Success(auth) =>
        println("Writing...")
        Freesound.writeAuth()(auth)
        sys.exit()
      case Failure(ex) =>
        println("refreshAuth failed:")
        ex.printStackTrace()
        sys.exit(1)
    }
  }
}
