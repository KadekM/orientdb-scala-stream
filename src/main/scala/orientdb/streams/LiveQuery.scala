package orientdb.streams

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery
import org.reactivestreams.Publisher
import orientdb.streams.impl._

import scala.reflect.ClassTag

trait LiveQuery[A] {
  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx): Publisher[A]
}

object LiveQuery {
  def apply[A: ClassTag](query: String)(implicit system: ActorSystem): LiveQuery[A] =
    new LiveQueryImpl[A](query)
}
