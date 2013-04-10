package controllers

import play.api.mvc.{Action, Controller}
import com.bwater.notebook.server.{ScalaNotebookConfig, NotebookSession}
import play.api.libs.json.Json

object NotebookController extends Controller with NotebookSession {

  val domain = "127.0.0.1"
  val port = 9000

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

  def view(name: String) = Action { implicit request =>
    val id = request.queryString.get("id").flatMap(_.headOption).getOrElse(nbm.notebookId(name))
    val wsUrl = "ws:/%s:%d".format(domain, port)

    Ok(views.html.notebook(nbm.name, id, name, wsUrl))
  }

  protected def config = ScalaNotebookConfig.defaults
}
