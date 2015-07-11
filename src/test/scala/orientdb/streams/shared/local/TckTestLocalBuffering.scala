package orientdb.streams.shared.local

import orientdb.streams.OverflowStrategy.Fail
import orientdb.streams.{NonBlockingQueryBuffering, NonBlockingQuery}
import orientdb.streams.shared.InMemoryTckTest

import scala.reflect.ClassTag

class TckTestLocalBuffering extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)(10000, Fail)
}
