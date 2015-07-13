package orientdb.streams

import akka.actor.ActorSystem
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.OverflowStrategy.OverflowStrategy
import orientdb.streams.impl._

trait LiveQuery {
  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, loader: OrientLoader): Publisher[LiveQueryData]
}

object LiveQuery {
  def apply(bufferSize: Int, overflowStrategy: OverflowStrategy)(query: String)(implicit system: ActorSystem): LiveQuery =
    new LiveQueryImpl(bufferSize, overflowStrategy)(query)
}
