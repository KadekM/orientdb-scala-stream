package orientdb.streams.shared.remote

import orientdb.streams.OverflowStrategy.Fail
import orientdb.streams.{NonBlockingQueryBuffering, NonBlockingQuery}
import orientdb.streams.shared.RemoteTckTest

import scala.reflect.ClassTag

class TckTestRemoteBuffering extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)(10000, Fail)
}
