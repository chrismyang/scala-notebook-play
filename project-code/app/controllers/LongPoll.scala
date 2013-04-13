package controllers

import play.api.libs
import libs.json._
import play.api.Play.current
import akka.actor.{Actor, Props}
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import play.api.libs.concurrent._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.concurrent._

class LongPoll(val sessionId: String) {
  import LongPoll._

  private lazy val queue = Akka.system.actorOf(Props[Queue])

  def send(text: String) {
    queue ! Enqueue(text)
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

  private def waitForNewMessages(): Promise[List[String]] = {
    implicit val timeout = akka.util.Timeout(60 seconds) // needed for ask below
    queue.ask(Drain).mapTo[List[String]].asPromise
  }

  private def messagesToJson(messages: List[String]): JsObject = {
    val jsObs = messages.map( msg => JsObject(Seq("data" -> JsString(msg))) )
    JsObject(Seq("result" -> JsArray(jsObs)))
  }
}

object LongPoll {

  private case class Enqueue(message: String)
  private case object Drain
  private case class Messages(messages: List[String])

  private class Queue extends Actor {
    private var messages = List[String]()

    protected def receive = {
      case Enqueue(message) =>
        messages = messages :+ message

      case Drain =>
        sender ! messages
        messages = Nil
    }
  }
}
