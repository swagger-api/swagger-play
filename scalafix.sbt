semanticdbEnabled in ThisBuild := true

semanticdbVersion in ThisBuild := scalafixSemanticdb.revision

scalafixScalaBinaryVersion in ThisBuild := "2.13"

scalafixDependencies in ThisBuild ++= Seq("com.github.liancheng" %% "organize-imports" % "0.5.0")
