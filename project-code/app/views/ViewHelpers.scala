package views

import controllers.routes

object ViewHelpers {
  def snStatic(filename: String) = routes.ScalaNotebookAssets.at(filename)
}
