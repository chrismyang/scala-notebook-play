package controllers

import play.api.mvc.{WebSocket, Controller}
import play.api.Logger
import akka.actor.{ActorRef, Actor, Props}
import com.bwater.notebook._
import net.liftweb.json._
import com.bwater.notebook.kernel.remote.VMManager
import com.bwater.notebook.ObservableClientChange
import com.bwater.notebook.ObservableUpdate
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json.{Json, JsObject, JsString}

object ObservableController extends Controller {

  def vmManager = NotebookController.vmManager

  def router = NotebookController.kernelRouter

  def system = NotebookController.system

  def open(contextId: String) = WebSocket.using[String] { request =>
    Logger.info("Opening observable WebSocket")

    val socket = Enumerator.imperative[String]()

    system.actorOf(Props(new Actor {
      router ! Router.Put(contextId, context.self)

      val clientToVM = context.actorOf(Props(new GuardedActor {
        def guard = for {
          handler <- getType[ActorRef]
        } yield {
          case msg:ObservableClientChange =>
            handler ! msg
        }
      }).withDispatcher("akka.actor.default-stash-dispatcher"), "clientToVM")

      locally { // the locally actually matters; prevents serializability shenanigans
        val vmToClient = context.actorOf(Props(new Actor {
            def receive = {
              case ObservableUpdate(obsId, newValue) =>

                val respJson = JsObject(Seq(
                  "id" -> JsString(obsId),
                  "new_value" -> Json.parse(pretty(render(newValue)))
                ))

                socket.push(Json.stringify(respJson))
            }
          }), "vmToClient")

          vmManager.tell(VMManager.Spawn(contextId, Props(new ObservableHandler(vmToClient))), clientToVM)
      }

      def receive = {
        case occ: ObservableClientChange => clientToVM.forward(occ)
      }
    }))

    val in = Iteratee.foreach[String] { msg =>
      Logger.debug("Observable %s got message %s".format(contextId, msg))
//      val json = parse(msg)
//
//      for (JField("id", JString(id)) <- json;
//           JField("new_value", value) <- json) {
//        router ! Router.Forward(contextId, ObservableClientChange(id, value))
//      }
    }.mapDone { _ =>
      router ! Router.Remove(contextId)
    }

    (in, socket)
  }
}
