import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "scala-notebook-play"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.vert-x" % "vertx-core" % "1.2.3.final"
  )

  val snUri = uri("git://github.com/chrismyang/scala-notebook.git")

  // Making these transitive dependecies first-order because idea plugin can't seem to follow transitive dependencies
  lazy val snKernel = ProjectRef(snUri, "kernel")
  lazy val snSubprocess = ProjectRef(snUri, "subprocess")

  lazy val snServer = ProjectRef(snUri, "server")

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
  ).dependsOn(snServer, snSubprocess, snKernel)

}
