name := "swagger-play2"
version := "1.5.3.2"

checksums in update := Nil

scalaVersion:= "2.11.7"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.7.2",
  "org.slf4j"          % "slf4j-api"                  % "1.6.4",
  "io.swagger"         % "swagger-core"               % "1.5.8",
  "io.swagger"        %% "swagger-scala-module"       % "1.0.2",
  "com.typesafe.play" %% "routes-compiler"            % "2.5.4",
  "com.typesafe.play" %% "play-ebean"                 % "2.0.0"            % "test",
  "org.specs2"        %% "specs2-core"                % "3.6.6"            % "test",
  "org.specs2"        %% "specs2-mock"                % "3.6.6"            % "test",
  "org.specs2"        %% "specs2-junit"               % "3.6.6"            % "test",
  "org.mockito"        % "mockito-core"               % "1.9.5"            % "test")

mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.equals("logback.xml")) }

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishArtifact in Test := false
publishMavenStyle := true
pomIncludeRepository := { x => false }
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
organization := "io.swagger"
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
  </developers>
}

lazy val root = (project in file(".")).enablePlugins(PlayScala)
