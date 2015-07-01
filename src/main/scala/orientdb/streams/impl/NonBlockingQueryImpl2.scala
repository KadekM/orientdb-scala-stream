package orientdb.streams.impl

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.NonBlockingQuery

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

private[streams] class NonBlockingQueryImpl2[A: ClassTag](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: scala.collection.immutable.Map[Object, Object]) extends NonBlockingQuery[A] {

  // ask from db only what you need
  /*
  override def execute(args: Object*)(implicit db: ODatabaseDocumentTx): Publisher[A] = {
    val actorRef = system.actorOf(Props(new ActorSource[Object]))
    val listener = createListener(actorRef)
    val oQuery =
      new OSQLNonBlockingQuery[Object](query, limit, fetchPlan, arguments.asJava, listener)

    db.command(oQuery).execute(args)
    ActorPublisher[A](actorRef)
  }*/

  override def execute(args: Any*)(implicit db: ODatabaseDocumentTx,
    system: ActorSystem,
    ec: ExecutionContext): Publisher[A] = {
    ???
  }

}
