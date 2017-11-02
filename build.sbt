val baseName  = "ScalaFreesound"
val baseNameL = baseName.toLowerCase

val baseDescr = "A library for accessing freesound.org from Scala."

lazy val projectVersion = "1.5.0-SNAPSHOT"
lazy val mimaVersion    = "1.5.0" // used for migration-manager

lazy val commonSettings = Seq(
  version               := projectVersion,
  organization          := "de.sciss",
  scalaVersion          := "2.12.4",
  crossScalaVersions    := Seq("2.12.4", "2.11.11" /* , "2.10.6" */),
  homepage              := Some(url(s"https://github.com/Sciss/${name.value}")),
  licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint"),
  scalacOptions in (Test, console) := {
    val c = (scalacOptions in (Compile, compile)).value
    c.filterNot(_ == "-Xlint") :+ "-Xlint:-unused,_"
  }
) ++ publishSettings

// ---- core dependencies ----

val optionalVersion       = "1.0.0"
val processorVersion      = "0.4.1"
val dispatchVersion       = "0.12.3" // note -- API changed: 0.13.1
val fileUtilVersion       = "1.1.3"
val serialVersion         = "1.0.3"

// ---- swing dependencies ----

val swingPlusVersion      = "0.2.4"
val raphaelVersion        = "1.0.4"

// ---- lucre dependencies ---

val soundProcessesVersion = "3.15.0-SNAPSHOT"
val fileCacheVersion      = "0.3.4"

// ---- compression dependencies ----

val audioFileVersion      = "1.4.6"   // PCM support
val jFLACVersion          = "1.5.2"   // FLAC support
val jump3rVersion         = "1.0.4"   // mp3 support
val jOrbisVersion         = "0.0.17"  // Ogg Vorbis support

// ---- test dependencies ----

val subminVersion         = "0.2.2"
val slf4jVersion          = "1.7.25"
val scalaTestVersion      = "3.0.4"

// ---- modules ----

lazy val core = project.in(file("core"))
  .settings(commonSettings)
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
      "de.sciss"                %% "serial"                 % serialVersion,
      "org.scalatest"           %% "scalatest"              % scalaTestVersion % "test"
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-core" % mimaVersion),
    initialCommands in (Test, console) := initialCmd()
  )

lazy val swing = project.in(file("swing"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name        := s"$baseName-swing",
    moduleName  := s"$baseNameL-swing",
    description := s"$baseDescr (Swing widgets)",
    libraryDependencies ++= Seq(
      "de.sciss" %% "swingplus"     % swingPlusVersion,
      "de.sciss" %% "raphael-icons" % raphaelVersion,
      "de.sciss" %  "submin"        % subminVersion % "test"
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-swing" % mimaVersion)
  )

lazy val lucre = project.in(file("lucre"))
  .dependsOn(swing)
  .settings(commonSettings)
  .settings(
    name        := s"$baseName-lucre",
    moduleName  := s"$baseNameL-lucre",
    description := s"$baseDescr (SoundProcesses integration)",
    libraryDependencies ++= Seq(
      "de.sciss" %% "soundprocesses-views" % soundProcessesVersion,
      "de.sciss" %% "filecache-txn"        % fileCacheVersion,
      "de.sciss" %  "submin"               % subminVersion % "test"
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-lucre" % mimaVersion)
  )

lazy val compression = project.in(file("compression"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name        := s"$baseName-compression",
    moduleName  := s"$baseNameL-compression",
    description := s"$baseDescr (decompression for FLAC, mp3, ogg)",
    libraryDependencies ++= Seq(
      "de.sciss"    %% "scalaaudiofile" % audioFileVersion,
      "org.jflac"   %  "jflac-codec"    % jFLACVersion,
      "de.sciss"    %  "jump3r"         % jump3rVersion,
      "org.jcraft"  %  "jorbis"         % jOrbisVersion,
      "org.slf4j"   %  "slf4j-nop"      % slf4jVersion % "test"
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-compression" % mimaVersion)
  )

lazy val root = project.in(file("."))
  .aggregate(core, swing, lucre, compression)
  .dependsOn(core, swing, lucre, compression)
  .settings(commonSettings)
  .settings(
    name        := baseName,
    moduleName  := baseNameL,
    description := baseDescr,
    initialCommands in (Test, console) := initialCmd(),
    publishArtifact in (Compile, packageBin) := false, // there are no binaries
    publishArtifact in (Compile, packageDoc) := false, // there are no javadocs
    publishArtifact in (Compile, packageSrc) := false  // there are no sources
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
  pomExtra := { val n = baseName
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
