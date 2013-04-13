package controllers

import akka.actor.Actor
import play.api.libs.concurrent.{Redeemable, Promise}
import java.util.concurrent.atomic.AtomicInteger
import play.api.Logger

class MessagingActor extends Actor {
  type MessagesPromise = Promise[List[Message]] with Redeemable[List[Message]]

  case class Member(promise: MessagesPromise)

  val seqCnt = new AtomicInteger()
  var messages = List[Message]()
  var members = Map.empty[String, Member]

  override def receive = {
    case BroadcastMessages() => {
      members.foreach {
        case (key, member) => {
          val newMessagesForMember = messages
          if (newMessagesForMember.size > 0) {
            member.promise.redeem(newMessagesForMember)
            members -= key
            Logger.info("Broadcasting "+newMessagesForMember.size+" msgs to " + key)
          }
        }
      }

      messages = Nil
    }

    case SendMessage(text) => {
      val msg = Message(text)
      messages = (msg :: messages)
      Logger.info("Added "+text+", seqId=="+seqCnt.get())
    }

    case ListenForMessages(clientId) => {
      val member =  Member(Promise[List[Message]]())
      members = members + (clientId -> member)

      Logger.info("Messages requested by clientId="+clientId)
      sender ! member.promise
    }

  }
}

case class Message(text: String)

case class SendMessage(text: String)
case class ListenForMessages(clientId: String)
case class BroadcastMessages()


