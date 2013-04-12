import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "scala-notebook-play"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
  )

//  lazy val depProject = ProjectRef(uri("git://github.com/chrismyang/scala-notebook.git#play_20"), "server")
  lazy val depProject = RootProject(uri("git://github.com/chrismyang/mustached-octo-lana.git"))

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
  ).dependsOn(depProject)

}
