package controllers

import play.api.mvc.{Action, Controller}

object NotebookController extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.projectdashboard())
  }

}
