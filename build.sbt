name                  := "ScalaFreesound"
version               := "1.0.0-SNAPSHOT"
organization          := "de.sciss"
scalaVersion          := "2.12.1"
crossScalaVersions    := Seq("2.12.1", "2.11.8" /* , "2.10.6" */)
description           := "A library for accessing freesound.org from Scala."
homepage              := Some(url(s"https://github.com/Sciss/${name.value}"))
licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint")

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.12")) Seq("-Xlint") else Nil
}

// ---- main dependencies ----

val optionalVersion   = "1.0.0"
val processorVersion  = "0.4.1"
val dispatchVersion   = "0.12.0"
val fileUtilVersion   = "1.1.2"

// ---- test dependencies ----

//val scoptVersion      = "3.5.0"
val slf4jVersion      = "1.7.7"

libraryDependencies ++= Seq(
  "de.sciss"                %% "optional"               % optionalVersion,
  "de.sciss"                %% "processor"              % processorVersion,
  "net.databinder.dispatch" %% "dispatch-core"          % dispatchVersion,
  "net.databinder.dispatch" %% "dispatch-json4s-native" % dispatchVersion, // dispatch-lift-json, dispatch-json4s-native, dispatch-json4s-jackson
  "de.sciss"                %% "fileutil"               % fileUtilVersion,
//  "com.github.scopt"        %% "scopt"                  % scoptVersion    % "test",
  "org.slf4j"               %  "slf4j-nop"              % slf4jVersion    % "test"
)

initialCommands in (Test, console) := {
  var res = 
    """import de.sciss.freesound._
      |val fs = Freesound  // alias
      |import Implicits._
      |import de.sciss.file._
      |import dispatch.Defaults.executor
      |type Vec[+A] = scala.collection.immutable.IndexedSeq[A]
      |val  Vec     = scala.collection.immutable.IndexedSeq
      |""".stripMargin

  if (file("client.json").exists) res ++=
    """implicit val client: Client = Freesound.readClient()
      |""".stripMargin

  if (file("auth.json").exists) res ++=
    """implicit val auth: Auth = Freesound.readAuth()
      |""".stripMargin
  
  res
}

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
