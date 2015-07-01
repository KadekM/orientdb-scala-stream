package orientdb.streams

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.impl._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait NonBlockingQuery[A] {
  def execute(args: Any*)(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext): Publisher[A]
}

object NonBlockingQueryBuffering {
  def apply[A: ClassTag](query: String,
               limit: Int = -1,
               fetchPlan: String = null,
               args: Map[Object, Object] = Map.empty[Object, Object])
              (implicit system: ActorSystem)  =
  new NonBlockingQueryBuffering[A](query, limit, fetchPlan, args)
}

object NonBlockingQueryLocking {
  // Just experimental, work in progress!
  def apply[A: ClassTag](query: String,
               limit: Int = -1,
               fetchPlan: String = null,
               args: Map[Object, Object] = Map.empty[Object, Object])
              (implicit system: ActorSystem)  =
  new NonBlockingQueryLocking[A](query, limit, fetchPlan, args)
}
