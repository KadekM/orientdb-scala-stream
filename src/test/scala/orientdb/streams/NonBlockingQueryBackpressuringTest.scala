package orientdb.streams

import akka.actor.ActorSystem

import scala.reflect.ClassTag

class NonBlockingQueryBackpressuringTest(_system: ActorSystem) extends NonBlockingQueryTest(_system) {
  def this() = this(ActorSystem("non-blocking-query-locking-test"))
  override def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBackpressuring[A](query)
}
