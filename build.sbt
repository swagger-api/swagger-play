name := "swagger-play2"
organization := "io.swagger"

scalaVersion := "2.13.1"

crossScalaVersions := Seq(scalaVersion.value, "2.12.10")

val PlayVersion = "2.8.1"
val SwaggerVersion = "1.6.0"
val Specs2Version = "4.8.3"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % PlayVersion,
  "com.typesafe.play" %% "routes-compiler" % PlayVersion,
  "io.swagger" % "swagger-core" % SwaggerVersion,
  "io.swagger" %% "swagger-scala-module" % "1.0.6",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.2",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3",
  "org.slf4j" % "slf4j-api" % "1.7.30",

  "com.typesafe.play" %% "play-ebean" % "5.0.2" % "test",
  "org.specs2" %% "specs2-core" % Specs2Version % "test",
  "org.specs2" %% "specs2-mock" % Specs2Version % "test",
  "org.specs2" %% "specs2-junit" % Specs2Version % "test",
  "org.mockito" % "mockito-core" % "3.2.0" % "test"
)

// see https://github.com/scala/bug/issues/11813
scalacOptions -= "-Wself-implicit"

scalacOptions in Test ~= filterConsoleScalacOptions

parallelExecution in Test := false // Swagger uses global state which breaks parallel tests

pomExtra := {
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
    <connection>scm:git:git@github.com:swagger-api/swagger-play.git</connection>
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
    <developer>
      <id>benmccann</id>
      <name>Ben McCann</name>
      <url>http://www.benmccann.com/</url>
    </developer>
    <developer>
      <id>frantuma</id>
      <name>Francesco Tumanischvili</name>
      <url>http://www.ft-software.net/</url>
    </developer>
    <developer>
      <id>gmethvin</id>
      <name>Greg Methvin</name>
      <url>https://methvin.net/</url>
    </developer>
  </developers>
}

publishTo := sonatypePublishTo.value

publishArtifact in Test := false
pomIncludeRepository := { _ => false }
publishMavenStyle := true
releaseCrossBuild := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
