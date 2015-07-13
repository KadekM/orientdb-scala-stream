package orientdb.stream

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import OverflowStrategy.OverflowStrategy
import orientdb.stream.impl.LiveQueryImpl

trait LiveQuery {
  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, loader: OrientLoader): Publisher[LiveQueryData]
}

object LiveQuery {
  def apply(bufferSize: Int, overflowStrategy: OverflowStrategy)(query: String)(implicit system: ActorSystem): LiveQuery =
    new LiveQueryImpl(bufferSize, overflowStrategy)(query)
}


