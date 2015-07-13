package orientdb.stream.shared.remote

import orientdb.stream.shared.RemoteTckTest
import orientdb.stream.{NonBlockingQueryBuffering, NonBlockingQuery, OverflowStrategy}
import OverflowStrategy.Fail
import scala.reflect.ClassTag

class TckTestRemoteBuffering extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](10000, Fail)(query)
}
