package orientdb.streams.shared.local

import orientdb.streams.shared.InMemoryTckTest
import orientdb.streams.{NonBlockingQuery, NonBlockingQueryLocking}

import scala.reflect.ClassTag

class TckTestLocalLocking extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryLocking[A](query)
}
