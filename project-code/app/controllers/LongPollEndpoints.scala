package controllers

import scala.collection.mutable
import scala.util.Random

class LongPollEndpoints {

  private val sockets = mutable.Map[String, LongPoll]()

  def open(): LongPoll = {
    val sessionId = generateSessionId
    val socket = new LongPoll(sessionId)
    this.sockets.put(sessionId, socket)
    socket
  }

  def get(sessionId: String): Option[LongPoll] = sockets.get(sessionId)

  private def generateSessionId = Random.alphanumeric.take(20).mkString
}
