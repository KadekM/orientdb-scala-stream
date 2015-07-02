package orientdb.streams

import akka.actor.ActorSystem

import scala.reflect.ClassTag

class NonBlockingQueryBufferingTest(_system: ActorSystem) extends NonBlockingQueryTest(_system) {
  def this() = this(ActorSystem("non-blocking-query-buffering"))
  override def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)
}
