package orientdb.stream.shared.local

import orientdb.stream.shared.InMemoryTckTest
import orientdb.stream.{NonBlockingQueryBuffering, NonBlockingQuery, OverflowStrategy}
import OverflowStrategy.Fail

import scala.reflect.ClassTag

class TckTestLocalBuffering extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](10000, Fail)(query)
}
