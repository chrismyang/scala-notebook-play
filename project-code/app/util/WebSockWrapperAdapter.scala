package util

import com.bwater.notebook.server.WebSockWrapper
import play.api.libs.iteratee.PushEnumerator

class WebSockWrapperAdapter(out: PushEnumerator[String]) extends WebSockWrapper(null) {
  override def send(msg: String) {
    out.push(msg)
  }
}
