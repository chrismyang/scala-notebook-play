package controllers

import play.api.mvc.{Action, Controller}
import com.bwater.notebook.server.{ScalaNotebookConfig, NotebookSession}
import play.api.libs.json.Json

object NotebookController extends Controller with NotebookSession {



  def index = Action { implicit request =>
    val projectName = "."
    Ok(views.html.projectdashboard(projectName))
  }

  def listNotebooks = Action { implicit request =>
    val jsonString = unfiltered.response.Json.jsonToString(nbm.listNotebooks)
    Ok(Json.parse(jsonString))
  }

  def clusters = Action { implicit request =>
    val s = """[{"profile":"default","status":"stopped","profile_dir":"C:\\Users\\Ken\\.ipython\\profile_default"}]"""
    Ok(Json.parse(s))
  }

  protected def config = ScalaNotebookConfig.defaults
}
