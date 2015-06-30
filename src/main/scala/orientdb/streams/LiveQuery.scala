package orientdb.streams

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery
import org.reactivestreams.Publisher
import orientdb.streams.impl._

trait LiveQuery[A] {
  def execute(args: Object*)(implicit db: ODatabaseDocumentTx): Publisher[A]
}

object LiveQuery {
  def apply[A](query: String)(implicit system: ActorSystem): LiveQuery[A] =
    new LiveQueryImpl[A](query)
}
