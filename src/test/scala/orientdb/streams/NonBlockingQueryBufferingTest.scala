package orientdb.streams

import akka.actor.ActorSystem
import orientdb.streams.OverflowStrategy.DropTail

import scala.reflect.ClassTag

class NonBlockingQueryBufferingTest(_system: ActorSystem) extends NonBlockingQueryTest(_system) {
  def this() = this(ActorSystem("non-blocking-query-buffering"))
  override def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)(10000, DropTail)
}
