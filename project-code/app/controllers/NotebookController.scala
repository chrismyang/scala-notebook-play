package controllers

import play.api.mvc.{Action, Controller}

object NotebookController extends Controller {
  def index = Action { implicit request =>
    val projectName = "TODO Project Name"
    Ok(views.html.projectdashboard(projectName))
  }

}
