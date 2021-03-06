package orientdb.stream

import akka.actor.ActorSystem
import OverflowStrategy.DropTail

import scala.reflect.ClassTag

class NonBlockingQueryBufferingTest(_system: ActorSystem) extends NonBlockingQueryTest(_system) {
  def this() = this(ActorSystem("non-blocking-query-buffering"))
  override def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](10000, DropTail)(query)
}
