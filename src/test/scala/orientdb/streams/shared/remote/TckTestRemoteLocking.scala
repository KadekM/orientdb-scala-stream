package orientdb.streams.shared.remote

import orientdb.streams.shared.RemoteTckTest
import orientdb.streams.{NonBlockingQuery, NonBlockingQueryLocking}

import scala.reflect.ClassTag

class TckTestRemoteLocking extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryLocking[A](query)
}
