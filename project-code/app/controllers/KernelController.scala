package controllers

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import com.bwater.notebook.server._
import com.bwater.notebook.Router
import akka.actor.{ActorRef, Props}
import unfiltered.response.{ResponseString, JsonContent}
import net.liftweb.json._
import unfiltered.response.ResponseString
import java.util.UUID
import com.bwater.notebook.kernel.remote.VMManager
import play.api.libs.json._
import play.api.libs.concurrent.{Promise, Akka}
import play.api.Play.current
import play.api.libs.iteratee.{Enumerator, Iteratee}
import util.WebSockWrapperAdapter
import com.bwater.notebook.server.IopubChannel
import com.bwater.notebook.server.ShellChannel
import com.bwater.notebook.client.{ObjectInfoRequest, CompletionRequest, ExecuteRequest}
import net.liftweb.json
import java.util.concurrent.atomic.AtomicInteger
import com.bwater.notebook.client.ObjectInfoRequest
import com.bwater.notebook.client.CompletionRequest
import play.api.libs.json.JsString
import com.bwater.notebook.client.ExecuteRequest
import com.bwater.notebook.server.IopubChannel
import com.bwater.notebook.server.SessionRequest
import com.bwater.notebook.server.ShellChannel
import play.api.libs.json.JsObject

object KernelController extends Controller {
  def config = NotebookController.config

  def system = NotebookController.system

  def start = Action { implicit request =>
    Logger.info("Starting kernel")
    startKernel(UUID.randomUUID.toString, "ws", request.host)
  }


  private def startKernel(kernelId: String, protocol: String, host: String) = {
    val compilerArgs = config.kernelCompilerArgs
    val initScripts = config.kernelInitScripts
    // Load the user script from disk every time, so user changes are applied whenever a kernel is started/restarted.
    def kernelMaker = new Kernel(initScripts, compilerArgs, remoteSpawner(kernelId))

    //TODO: this is a potential memory leak, if the websocket is never opened the router will never be removed...
    kernelRouter ! Router.Put(kernelId, system.actorOf(Props(kernelMaker).withDispatcher("akka.actor.default-stash-dispatcher")))

    val json = JsObject(Seq(
      "kernel_id" -> JsString(kernelId),
      "ws_url" -> JsString("%s:/%s".format(protocol, host))
    ))

    Ok(json)
  }

  def open(kernelId: String, channel: String) = WebSocket.using[String] { request =>

    val out = Enumerator.imperative[String]()

    openSocket(kernelId, channel, new WebSockWrapperAdapter(out))

    val in = Iteratee.foreach[String] { msg =>
      onSocketMessage(kernelId, msg)
    }.mapDone { _ =>
      onSocketClose(kernelId, request.uri)
    }

    (in, out)
  }

  private def openSocket(kernelId: String, channel: String, socket: WebSockWrapper) {
    Logger.info("Opening Socket %s for %s to %s".format(channel, kernelId, ""))

    if (channel == "iopub")
      kernelRouter ! Router.Forward(kernelId, IopubChannel(socket))
    else if (channel == "shell")
      kernelRouter ! Router.Forward(kernelId, ShellChannel(socket))
  }

  private def onSocketMessage(kernelId: String, msg: String) {
    Logger.debug("Message for %s:%s".format(kernelId, msg))
    dispatchRequest(msg, kernelId)
  }

  private def onSocketClose(kernelId: String, requestUri: String) {
    Logger.info("Closing socket " + requestUri)

    vmManager ! VMManager.Kill(kernelId)
    kernelRouter ! Router.Remove(kernelId)
  }

  def kernelRouter = NotebookController.kernelRouter

  def vmManager = NotebookController.vmManager

  val executionCounter = new AtomicInteger(0)

  def remoteSpawner(key: Any)(props: Props, replyTo: ActorRef) { vmManager.tell(VMManager.Start(key, config.notebooksDir), replyTo); vmManager.tell(VMManager.Spawn(key, props), replyTo)}

  private def dispatchRequest(requestMessage: String, kernelId: String) {
    def sendRequest(request: Any) {
      kernelRouter ! Router.Forward(kernelId, request)
    }

    val json = net.liftweb.json.parse(requestMessage)

    for {
      JField("header", header) <- json
      JField("session", session) <- header
      JField("msg_type", msgType) <- header
      JField("content", content) <- json
    } {
      msgType match {
        case JString("execute_request") => {
          for (JField("code", JString(code)) <- content) {
            val execCounter = executionCounter.incrementAndGet()
            sendRequest(SessionRequest(header, session, ExecuteRequest(execCounter, code)))
          }
        }

        case JString("complete_request") => {
          for (
            JField("line", JString(line)) <- content;
            JField("cursor_pos", JInt(cursorPos)) <- content
          ) {

            sendRequest(SessionRequest(header, session, CompletionRequest(line, cursorPos.toInt)))
          }
        }

        case JString("object_info_request") => {
          for (JField("oname", JString(oname)) <- content) {
            sendRequest(SessionRequest(header, session, ObjectInfoRequest(oname)))
          }
        }

        case x => Logger.warn("Unrecognized websocket message: " + requestMessage) //throw new IllegalArgumentException("Unrecognized message type " + x)
      }
    }
  }
}
