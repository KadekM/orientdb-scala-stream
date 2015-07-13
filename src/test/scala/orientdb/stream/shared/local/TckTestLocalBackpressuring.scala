package orientdb.stream.shared.local

import orientdb.stream.shared.InMemoryTckTest
import orientdb.stream.{NonBlockingQueryBackpressuring, NonBlockingQuery}

import scala.reflect.ClassTag

class TckTestLocalBackpressuring extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBackpressuring[A](query)
}
