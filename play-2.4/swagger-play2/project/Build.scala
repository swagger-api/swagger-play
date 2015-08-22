import sbt._
import Keys._
import play.sbt.Play.autoImport._
import play.sbt.PlayScala
import PlayKeys._
import play.ebean.sbt.PlayEbean

object ApplicationBuild extends Build {
  val appName = "swagger-play2"
  val appVersion = "1.3.13"

  checksums in update := Nil

  scalaVersion:= "2.11.6"

  val appDependencies = Seq(
    "org.slf4j"          % "slf4j-api"       % "1.6.4",
    "com.wordnik"       %% "swagger-jaxrs"   % "1.3.12",
    "javax.ws.rs"        % "jsr311-api"      % "1.1.1",
    "org.specs2"        %% "specs2-core"     % "3.6"              % "test",
    // "org.specs2"        %% "specs2-core"     % "3.6"              % "test",
    "org.specs2"        %% "specs2-mock"     % "3.6"              % "test",
    "org.mockito"        % "mockito-core"    % "1.9.5"            % "test",
    "com.typesafe.play" %% "play-java-ebean" % "2.3.0"            % "test")

  val main = Project(appName, file(".")).enablePlugins(PlayScala, PlayEbean).settings(
    crossScalaVersions := Seq("2.11.6", "2.11.7"),
    scalaVersion := "2.11.6",
    version := appVersion,
    libraryDependencies ++= appDependencies,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    publishMavenStyle := true,
    pomIncludeRepository := { x => false },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    organization := "com.wordnik",
    pomExtra :=
      <url>http://swagger.io</url>
      <licenses>
        <license>
          <name>Apache License 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:swagger-api/swagger-play.git</url>
        <connection>scm:git:git@github.com:swagger-api/swagger-core.git</connection>
      </scm>
      <developers>
        <developer>
          <id>fehguy</id>
          <name>Tony Tam</name>
          <email>fehguy@gmail.com</email>
        </developer>
        <developer>
          <id>ayush</id>
          <name>Ayush Gupta</name>
          <email>ayush@glugbot.com</email>
        </developer>
        <developer>
          <id>rayyildiz</id>
          <name>Ramazan AYYILDIZ</name>
          <email>rayyildiz@gmail.com</email>
        </developer>
      </developers>,
    resolvers := Seq(
      "maven-central" at "https://repo1.maven.org/maven2",
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases",
      "java-net" at "http://download.java.net/maven/2",
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
      "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"))
}
