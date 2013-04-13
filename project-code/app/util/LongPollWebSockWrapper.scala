package util

import com.bwater.notebook.server.WebSockWrapper
import controllers.LongPoll

class LongPollWebSockWrapper(longPollSocket: LongPoll) extends WebSockWrapper(null) {
  override def send(msg: String) {
    longPollSocket.send(msg)
  }
}
