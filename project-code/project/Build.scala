import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "scala-notebook-play"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.bwater" % "notebook-server_2.9.2" % "0.3.0-SNAPSHOT"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Local SBT Repository" at "file:///Users/chris/.ivy2/local"
  )

}
