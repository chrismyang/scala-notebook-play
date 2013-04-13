package controllers

import play.api.libs
import libs.json._
import play.api.Play.current
import scala.util.Random
import akka.actor.Props
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import play.api.libs.concurrent._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

class LongPoll(val sessionId: String) {

  def send(text: String) {
    messagingActor ! SendMessage(text)
  }

  def poll(): Promise[JsValue] = {

    val promiseOfMessages = waitForNewMessages()

    val promiseOfResult = {
      promiseOfMessages.orTimeout("Timeout", 60000).map { eitherMessagesOrTimeout =>
        eitherMessagesOrTimeout.fold(
          messages => messagesToJson(messages),
          timeout => messagesToJson(List.empty)
        )
      }
    }

    promiseOfResult
  }

  private def waitForNewMessages(): Promise[List[Message]] = {
    implicit val timeout = akka.util.Timeout(60 seconds) // needed for ask below
    Await.result(messagingActor.ask(ListenForMessages(rndClientId)).mapTo[Promise[List[Message]]], 60 seconds)
  }

  private def rndClientId = Random.nextInt(999999).toString()

  private def messagesToJson(messages: List[Message]): JsObject = {
    val jsObs = messages.map( msg => JsObject(Seq("data" -> JsString(msg.text))) )
    JsObject(Seq("result" -> JsArray(jsObs)))
  }

  lazy val messagingActor = {
    val actor = Akka.system.actorOf(Props[MessagingActor])

    // Tell the actor to broadcast messages every 1 second
    Akka.system.scheduler.schedule(0 seconds, 1 seconds, actor, BroadcastMessages())


    actor
  }
}
