val baseName  = "ScalaFreesound"
val baseNameL = baseName.toLowerCase

val baseDescr = "A library for accessing freesound.org from Scala."

lazy val commonSettings = Seq(
  version               := "1.0.0-SNAPSHOT",
  organization          := "de.sciss",
  scalaVersion          := "2.12.1",
  crossScalaVersions    := Seq("2.12.1", "2.11.8" /* , "2.10.6" */),
  homepage              := Some(url(s"https://github.com/Sciss/${name.value}")),
  licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint"),
  scalacOptions ++= {
    if (scalaVersion.value.startsWith("2.12")) Seq("-Xlint") else Nil
  }
)

// ---- core dependencies ----

val optionalVersion       = "1.0.0"
val processorVersion      = "0.4.1"
val dispatchVersion       = "0.12.0"
val fileUtilVersion       = "1.1.2"

// ---- swing dependencies ----

val swingPlusVersion      = "0.2.2"
val raphaelVersion        = "1.0.4"

// ---- lucre dependencies ---

val soundProcessesVersion = "3.11.0"
val fileCacheVersion      = "0.3.4"

// ---- test dependencies ----

//val scoptVersion         = "3.5.0"
val subminVersion         = "0.2.1"
val slf4jVersion          = "1.7.7"

// ---- modules ----

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name        := s"$baseName-core",
    moduleName  := s"$baseNameL-core",
    description := s"$baseDescr (core module)",
    libraryDependencies ++= Seq(
      "de.sciss"                %% "optional"               % optionalVersion,
      "de.sciss"                %% "processor"              % processorVersion,
      "net.databinder.dispatch" %% "dispatch-core"          % dispatchVersion,
      "net.databinder.dispatch" %% "dispatch-json4s-native" % dispatchVersion, // dispatch-lift-json, dispatch-json4s-native, dispatch-json4s-jackson
      "de.sciss"                %% "fileutil"               % fileUtilVersion,
      //  "com.github.scopt"        %% "scopt"                  % scoptVersion    % "test",
      "org.slf4j"               %  "slf4j-nop"              % slf4jVersion    % "test"
    ),
    initialCommands in (Test, console) := initialCmd()
  )

lazy val swing = project.in(file("swing"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name        := s"$baseName-swing",
    moduleName  := s"$baseNameL-swing",
    description := s"$baseDescr (Swing widgets)",
    libraryDependencies ++= Seq(
      "de.sciss" %% "swingplus"     % swingPlusVersion,
      "de.sciss" %% "raphael-icons" % raphaelVersion,
      "de.sciss" %  "submin"        % subminVersion % "test"
    )
  )

lazy val lucre = project.in(file("lucre"))
  .dependsOn(swing)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name        := s"$baseName-lucre",
    moduleName  := s"$baseNameL-lucre",
    description := s"$baseDescr (SoundProcesses integration)",
    libraryDependencies ++= Seq(
      "de.sciss" %% "soundprocesses-views" % soundProcessesVersion,
      "de.sciss" %% "filecache-mutable"    % fileCacheVersion
    )
  )

lazy val root = project.in(file("."))
  .dependsOn(core, swing, lucre)
  .aggregate(core, swing, lucre)
  .settings(commonSettings)
  .settings(
    name        := baseName,
    moduleName  := baseNameL,
    description := baseDescr,
    initialCommands in (Test, console) := initialCmd(),
    packagedArtifacts := Map.empty           // prevent publishing anything!
  )

def initialCmd(): String = {
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

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
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
)
