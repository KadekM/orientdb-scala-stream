package orientdb.stream

import akka.actor.ActorRefFactory
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import OverflowStrategy.OverflowStrategy
import orientdb.stream.impl.LiveQueryImpl

trait LiveQuery {
  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, actorRefFactory: ActorRefFactory, loader: OrientLoader): Publisher[LiveQueryData]
}

object LiveQuery {
  def apply(bufferSize: Int, overflowStrategy: OverflowStrategy)(query: String): LiveQuery =
    new LiveQueryImpl(bufferSize, overflowStrategy)(query)
}

