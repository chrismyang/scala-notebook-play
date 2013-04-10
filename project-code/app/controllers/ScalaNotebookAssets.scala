package controllers

import play.api.mvc.{ResponseHeader, SimpleResult, Action, Controller}
import play.api.libs.MimeTypes
import play.api.Play
import play.api.libs.iteratee.Enumerator
import play.api.Play.current
import java.net.URL

object ScalaNotebookAssets extends Controller {

  def at(filename: String) = Action { implicit request =>
    // Essentially forked from Assets.at -- but couldn't figure out a good way to reuse it, but could just be my own
    // incompetence
    (for {
      url <- tryLoadFileFromScalaNotebookRoots(filename)
    } yield {

      lazy val (length, resourceData) = openStream(url)

      val response = buildResult(length, resourceData, MimeTypes.forFileName(filename))

      response
    }).getOrElse(NotFound)
  }

  private def tryLoadFileFromScalaNotebookRoots(filename: String) = {
    tryLoadFile(FromIPythonRoot, filename) orElse tryLoadFile(ThirdPartyRoot, filename)
  }

  private def tryLoadFile(root: String, filename: String) = {
    val fullPath = root + "/" + filename
    Play.resource(fullPath)
  }

  private def openStream(url: URL) = {
    val stream = url.openStream()
    try {
      (stream.available, Enumerator.fromStream(stream))
    } catch {
      case _ => (0, Enumerator[Array[Byte]]())
    }
  }

  private def buildResult(length: Int, resourceData: Enumerator[Array[Byte]], fileType: Option[String]) = {
    SimpleResult(
      header = ResponseHeader(OK, Map(
        CONTENT_LENGTH -> length.toString,
        CONTENT_TYPE -> fileType.getOrElse(BINARY)
      )),
      resourceData
    )
  }

  private val FromIPythonRoot = "from_ipython/static"
  private val ThirdPartyRoot = "thirdparty/static"
}
