package orientdb.streams.shared.local

import orientdb.streams.shared.InMemoryTckTest
import orientdb.streams.{NonBlockingQuery, NonBlockingQueryBackpressuring}

import scala.reflect.ClassTag

class TckTestLocalBackpressuring extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBackpressuring[A](query)
}
