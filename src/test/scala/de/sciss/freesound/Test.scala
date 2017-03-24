package de.sciss.freesound

import scala.concurrent.ExecutionContext

object Test {
  def main(args: Array[String]): Unit = {
    val token = args.headOption.getOrElse(sys.error("Need to specify API key"))
    val fs    = Freesound(token)
    val fut   = fs run TextSearch("fish")

    import ExecutionContext.Implicits.global

    fut.onComplete { res =>
      println(res)
      sys.exit(if (res.isSuccess) 0 else 1)
    }
  }
}
