package controllers

import play.api.mvc.{WebSocket, Action, Controller}
import play.api.Logger
import com.bwater.notebook.server.{ShellChannel, WebSockWrapper, IopubChannel, Kernel}
import com.bwater.notebook.Router
import akka.actor.{ActorRef, Props}
import unfiltered.response.{ResponseString, JsonContent}
import net.liftweb.json._
import unfiltered.response.ResponseString
import java.util.UUID
import com.bwater.notebook.kernel.remote.VMManager
import play.api.libs.json.{JsString, JsObject}
import play.api.libs.concurrent.{Promise, Akka}
import play.api.Play.current
import play.api.libs.iteratee.{Enumerator, Iteratee}
import util.WebSockWrapperAdapter

object KernelController extends Controller {
  def config = NotebookController.config

  def start = Action { implicit request =>
    Logger.info("Starting kernel")
    startKernel(UUID.randomUUID.toString)
  }


  private def startKernel(kernelId: String) = {
    val compilerArgs = config.kernelCompilerArgs
    val initScripts = config.kernelInitScripts
    // Load the user script from disk every time, so user changes are applied whenever a kernel is started/restarted.
    def kernelMaker = new Kernel(initScripts, compilerArgs, remoteSpawner(kernelId))

    //TODO: this is a potential memory leak, if the websocket is never opened the router will never be removed...
    kernelRouter ! Router.Put(kernelId, Akka.system.actorOf(Props(kernelMaker).withDispatcher("akka.actor.default-stash-dispatcher")))

    val json = JsObject(Seq(
      "kernel_id" -> JsString(kernelId),
      "ws_url" -> JsString("ws:/%s:%d".format(domain, port))
    ))

    Ok(json)
  }

  def open(kernelId: String, channel: String) = WebSocket.using[String] { request =>
    def sendRequest(request: Any) {
      kernelRouter ! Router.Forward(kernelId, request)
    }

    Logger.info("Opening Socket %s for %s to %s".format(channel, kernelId, ""))

    val out = Enumerator.imperative[String]()

    if (channel == "iopub")
      kernelRouter ! Router.Forward(kernelId, IopubChannel(new WebSockWrapperAdapter(out)))
    else if (channel == "shell")
      kernelRouter ! Router.Forward(kernelId, ShellChannel(new WebSockWrapperAdapter(out)))


    val in = Iteratee.foreach[String] { msg =>
      Logger.debug("Message for %s:%s".format(kernelId, msg))

      // TODO parse message and dispatch correctly

    }.mapDone { _ =>
      Logger.info("Closing socket " + request.uri)

      vmManager ! VMManager.Kill(kernelId)
      kernelRouter ! Router.Remove(kernelId)
    }

    (in, out)
  }

  def domain = NotebookController.domain

  def port = NotebookController.port

  def kernelRouter = NotebookController.kernelRouter

  def vmManager = NotebookController.vmManager

  def remoteSpawner(key: Any)(props: Props, replyTo: ActorRef) { vmManager.tell(VMManager.Start(key, config.notebooksDir), replyTo); vmManager.tell(VMManager.Spawn(key, props), replyTo)}

}
