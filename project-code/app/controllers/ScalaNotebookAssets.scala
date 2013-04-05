package controllers

import play.api.mvc.{Action, Controller}
import com.bwater.notebook.server.Dispatcher
import org.apache.commons.io.IOUtils

object ScalaNotebookAssets extends Controller {

  def at(filename: String) = Action { implicit request =>

    (for {
      fileStream <- tryLoadFileFromScalaNotebookRoots(filename)
    } yield {
      val file = IOUtils.toString(fileStream)
      Ok(file)
    }).getOrElse(NotFound)
  }

  private def tryLoadFileFromScalaNotebookRoots(filename: String) = {
    tryLoadFile(FromIPythonRoot, filename) orElse tryLoadFile(ThirdPartyRoot, filename)
  }

  private def tryLoadFile(root: String, filename: String) = {
    val fullPath = root + "/" + filename
    val stream = serverClassLoader.getResourceAsStream(fullPath)
    Option(stream)
  }

  private def serverClassLoader = classOf[Dispatcher].getClassLoader

  private val FromIPythonRoot = "from_ipython/static"
  private val ThirdPartyRoot = "thirdparty/static"
}
