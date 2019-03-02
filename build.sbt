val baseName  = "ScalaFreesound"
val baseNameL = baseName.toLowerCase

val baseDescr = "A library for accessing freesound.org from Scala."

lazy val projectVersion = "1.14.0-SNAPSHOT"
lazy val mimaVersion    = "1.14.0" // used for migration-manager

lazy val commonSettings = Seq(
  version               := projectVersion,
  organization          := "de.sciss",
  scalaVersion          := "2.12.8",
  crossScalaVersions    := Seq("2.12.8", "2.11.12"),
  homepage              := Some(url(s"https://github.com/Sciss/${name.value}")),
  licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalacOptions        ++= Seq(
    "-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint", "-Xsource:2.13"
  ),
  scalacOptions in (Test, console) := {
    val c = (scalacOptions in (Compile, compile)).value
    c.filterNot(_ == "-Xlint") :+ "-Xlint:-unused,_"
  },
  updateOptions := updateOptions.value.withLatestSnapshots(false)
) ++ publishSettings

lazy val deps = new {
  val core = new {
    val optional       = "1.0.0"
    val processor      = "0.4.2"
    val dispatch       = "0.12.3" // note -- API changed: 0.13.1
    val fileUtil       = "1.1.3"
    val serial         = "1.1.1"
  }
  val swing = new {
    val swingPlus      = "0.4.0"
    val raphael        = "1.0.5"
  }
  val lucre = new {
    val soundProcesses = "3.25.0-SNAPSHOT"
    val fileCache      = "0.5.0"
  }
  val compression = new {
    val audioFile      = "1.5.1"   // PCM support
    val jFLAC          = "1.5.2"   // FLAC support
    val jump3r         = "1.0.5"   // mp3 support
    val jOrbis         = "0.0.17"  // Ogg Vorbis support
  }
  val test = new {
    val submin         = "0.2.4"
    val slf4j          = "1.7.26"
    val scalaTest      = "3.0.6"
  }
}

// ---- modules ----

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name        := s"$baseName-core",
    moduleName  := s"$baseNameL-core",
    description := s"$baseDescr (core module)",
    libraryDependencies ++= Seq(
      "de.sciss"                %% "optional"               % deps.core.optional,
      "de.sciss"                %% "processor"              % deps.core.processor,
      "net.databinder.dispatch" %% "dispatch-core"          % deps.core.dispatch,
      "net.databinder.dispatch" %% "dispatch-json4s-native" % deps.core.dispatch, // dispatch-lift-json, dispatch-json4s-native, dispatch-json4s-jackson
      "de.sciss"                %% "fileutil"               % deps.core.fileUtil,
      "de.sciss"                %% "serial"                 % deps.core.serial,
      "org.scalatest"           %% "scalatest"              % deps.test.scalaTest % "test"
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
      "de.sciss" %% "swingplus"     % deps.swing.swingPlus,
      "de.sciss" %% "raphael-icons" % deps.swing.raphael,
      "de.sciss" %  "submin"        % deps.test.submin % "test"
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
      "de.sciss" %% "soundprocesses-views" % deps.lucre.soundProcesses,
      "de.sciss" %% "filecache-txn"        % deps.lucre.fileCache,
      "de.sciss" %  "submin"               % deps.test.submin % "test"
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
      "de.sciss"    %% "audiofile"      % deps.compression.audioFile,
      "org.jflac"   %  "jflac-codec"    % deps.compression.jFLAC,
      "de.sciss"    %  "jump3r"         % deps.compression.jump3r,
      "org.jcraft"  %  "jorbis"         % deps.compression.jOrbis,
      "org.slf4j"   %  "slf4j-nop"      % deps.test.slf4j % "test"
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
