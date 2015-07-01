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

  override def execute(args: Any*)(implicit db: ODatabaseDocumentTx,
    system: ActorSystem,
    ec: ExecutionContext): Publisher[A] = {
    ???
  }
}
