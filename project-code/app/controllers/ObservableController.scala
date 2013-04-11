package controllers

import play.api.mvc.{WebSocket, Controller}
import play.api.Logger
import akka.actor.{ActorRefFactory, ActorRef, Actor, Props}
import com.bwater.notebook._
import net.liftweb.json._
import com.bwater.notebook.kernel.remote.VMManager
import com.bwater.notebook.ObservableClientChange
import com.bwater.notebook.ObservableUpdate
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.json.{Json, JsObject, JsString}
import akka.japi.Creator

object ObservableController extends Controller {

  def vmManager = NotebookController.vmManager

  def system = NotebookController.system

  val obs = new ObservableFoo(vmManager, system)

  def open(contextId: String) = WebSocket.using[String] { request =>
    obs.open(contextId)
  }
}

class ObservableFoo(vmManager: ActorRef, system: ActorRefFactory) {

  private val router = system.actorOf(Props[Router])

  def open(contextId: String) = {
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

        val props = Props(new ObservableHandlerCreator(vmToClient))
        vmManager.tell(VMManager.Spawn(contextId, props), clientToVM)
      }

      def receive = {
        case occ: ObservableClientChange => clientToVM.forward(occ)
      }
    }))

    val in = Iteratee.foreach[String] { msg =>
      Logger.debug("Observable %s got message %s".format(contextId, msg))

      if (!msg.startsWith("s_nr")) {
        val json = parse(msg)

        for (JField("id", JString(id)) <- json;
             JField("new_value", value) <- json) {
          router ! Router.Forward(contextId, ObservableClientChange(id, value))
        }
      }
    }.mapDone { _ =>
      router ! Router.Remove(contextId)
    }

    (in, socket)
  }

  def makeGuy(vmToClient: ActorRef) = new ObservableHandler(vmToClient)

}

class ObservableHandlerCreator(vmToClient: ActorRef) extends Creator[ObservableHandler] with Serializable {
  def create() = new ObservableHandler(vmToClient)
}

class TestActor extends Actor {
  protected def receive = Actor.emptyBehavior
}