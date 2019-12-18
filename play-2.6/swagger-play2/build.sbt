name := "swagger-play2"
version := "1.6.2-SNAPSHOT"

checksums in update := Nil

scalaVersion := "2.11.12"

crossScalaVersions := Seq(scalaVersion.value, "2.12.6")

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.9.8",
  "org.slf4j"          % "slf4j-api"                  % "1.7.21",
  "io.swagger"         % "swagger-core"               % "1.5.22",
  "io.swagger"        %% "swagger-scala-module"       % "1.0.5",
  "com.typesafe.play" %% "routes-compiler"            % "2.6.0",
  "uk.com.robust-it"   % "cloning"                    % "1.9.2",
  "com.typesafe.play" %% "play-ebean"                 % "4.0.2"            % "test",
  "org.specs2"        %% "specs2-core"                % "3.8.7"            % "test",
  "org.specs2"        %% "specs2-mock"                % "3.8.7"            % "test",
  "org.specs2"        %% "specs2-junit"               % "3.8.7"            % "test",
  "org.mockito"        % "mockito-core"               % "1.9.5"            % "test")

mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.equals("logback.xml")) }

publishTo := {
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else
    Some("Sonatype Nexus Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}


publishArtifact in Test := false
publishMavenStyle := true
pomIncludeRepository := { x => false }
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
organization := "io.swagger"
resolvers += Resolver.sonatypeRepo("snapshots")
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
