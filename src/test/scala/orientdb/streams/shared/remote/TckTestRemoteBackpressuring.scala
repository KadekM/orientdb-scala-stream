package orientdb.streams.shared.remote

import orientdb.streams.shared.RemoteTckTest
import orientdb.streams.{NonBlockingQuery, NonBlockingQueryBackpressuring}

import scala.reflect.ClassTag

class TckTestRemoteBackpressuring extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBackpressuring[A](query)
}
