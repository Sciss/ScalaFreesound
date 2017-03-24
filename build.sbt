name                  := "ScalaFreesound"
version               := "1.0.0-SNAPSHOT"
organization          := "de.sciss"
scalaVersion          := "2.12.1"
crossScalaVersions    := Seq("2.12.1", "2.11.8" /* , "2.10.6" */)
description           := "A library for accessing freesound.org from Scala."
homepage              := Some(url(s"https://github.com/Sciss/${name.value}"))
licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8")

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.12")) Seq("-Xlint") else Nil
}

// ---- main dependencies ----

val modelVersion    = "0.3.3"
val optionalVersion = "1.0.0"
val xmlVersion      = "1.0.6"
val dispatchVersion = "0.12.0"

// ---- test dependencies

val scoptVersion  = "3.5.0"

libraryDependencies ++= Seq(
  "de.sciss"                %% "model"          % modelVersion,
  "de.sciss"                %% "optional"       % optionalVersion,
  "net.databinder.dispatch" %% "dispatch-core"  % dispatchVersion,
  "com.github.scopt"        %% "scopt"          % scoptVersion % "test"
)

libraryDependencies ++= {
  if (scalaVersion.value.startsWith("2.10")) Nil else Seq(
    "org.scala-lang.modules" %% "scala-xml" % xmlVersion
  )
}

initialCommands in console :=
  """import de.sciss.freesound._
    |import Implicits._
    |""".stripMargin

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}
