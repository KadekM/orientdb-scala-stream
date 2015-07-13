package orientdb.stream.shared.remote

import orientdb.stream.{NonBlockingQueryBackpressuring, NonBlockingQuery}
import orientdb.stream.shared.RemoteTckTest

import scala.reflect.ClassTag

class TckTestRemoteBackpressuring extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBackpressuring[A](query)
}
