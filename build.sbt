resolvers in ThisBuild += "Artima Maven Repository".at("https://repo.artima.com/releases")

val playVersion = "2.8.7"
val swaggerVersion = "2.1.7"

lazy val root = project.in(file(".")).enablePlugins(ScalafixPlugin).settings(name := "swagger-play")
  .settings(
    scalaVersion := "2.13.5",
    scalacOptions ~=
      (_.filterNot(Set(
        "-Wdead-code",
        "-Wunused:params",
        "-Ywarn-dead-code",
        "-Ywarn-unused:params",
        "-Ywarn-unused:patvars",
        "-Wunused:explicits"
      )))
  ).settings(
    libraryDependencies ++= Seq(
      "com.github.pureconfig"        %% "pureconfig"           % "0.14.0",
      "com.typesafe.play"            %% "play"                 % playVersion,
      "com.typesafe.play"            %% "routes-compiler"      % playVersion,
      "org.scala-lang.modules"       %% "scala-java8-compat"   % "0.9.1",
      "io.swagger.core.v3"            % "swagger-core"         % swaggerVersion,
      "io.swagger.core.v3"            % "swagger-annotations"  % swaggerVersion,
      "io.swagger.core.v3"            % "swagger-models"       % swaggerVersion,
      "io.swagger.core.v3"            % "swagger-jaxrs2"       % swaggerVersion,
      "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.3.0",
      "javax.ws.rs"                   % "javax.ws.rs-api"      % "2.0.1"
    )
  )

inThisBuild(List(
  organization := "io.kinoplan",
  homepage := Some(url("https://github.com/kinoplan/swagger-play")),
  licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
  developers :=
    List(Developer("kinoplan", "Kinoplan", "job@kinoplan.ru", url("https://kinoplan.tech"))),
  scmInfo := Some(ScmInfo(
    url("https://github.com/kinoplan/swagger-play"),
    "scm:git:git@github.com:kinoplan/swagger-play.git"
  ))
))
