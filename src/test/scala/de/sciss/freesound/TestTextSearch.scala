package de.sciss.freesound

object TestTextSearch {
  def main(args: Array[String]): Unit = {
    implicit val token: ApiKey = args.headOption.getOrElse(sys.error("Need to specify API key"))
    val fut   = Freesound.textSearch("fish", Filter(duration = 4 to 100, tags = "portugal"))

    import dispatch.Defaults.executor

    fut.onComplete { res =>
//      println(res)
      res.foreach { xs =>
        xs.foreach { snd =>
          println(snd)
          // val err = Try(new URI(snd.license)).isFailure
          // if (err) println(s"Cannot parse license URL '${snd.license}'")
          // if (snd.pack.isDefined) println(s"YES PACK ${snd.pack.get}")
          // if (snd.geoTag.isDefined) println(s"YES GEO ${snd.geoTag.get}")

          require(snd.fileName     != null)
          require(snd.tags         != null)
          require(snd.description  != null)
          require(snd.userName     != null)
        }
      }
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}
