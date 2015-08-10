// Comment to get more information during initialization
// logLevel := Level.Warn

resolvers ++= Seq(
    "Maven central" at "http://repo1.maven.org/maven2",
    "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    DefaultMavenRepository
)

addSbtPlugin("play" % "sbt-plugin" % "2.1.0")

// addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")
