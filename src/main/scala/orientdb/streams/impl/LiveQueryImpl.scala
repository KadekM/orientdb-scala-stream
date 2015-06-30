package orientdb.streams.impl

import akka.actor.{Props, ActorSystem}
import akka.stream.actor
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.ORecordOperation
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.{OLiveQuery, OLiveResultListener}
import org.reactivestreams.{Subscriber, Publisher}
import orientdb.streams.LiveQuery

trait CancellablePublisher[A] extends Publisher[A] {
  val token: Int
  def cancel()(implicit db: ODatabaseDocumentTx): Unit
}

object CancellablePublisher {
  def apply[A](publisher: Publisher[A], token: Int): CancellablePublisher[A] =
    new CancellablePublisherImpl[A](publisher, token)
}

class CancellablePublisherImpl[A](publisher: Publisher[A], val token: Int) extends CancellablePublisher[A] {
  override def cancel()(implicit db: ODatabaseDocumentTx): Unit =
    db.command(new OCommandSQL(s"live unsubscribe $token")).execute()

  override def subscribe(s: Subscriber[_ >: A]): Unit = publisher.subscribe(s)
}

private[streams] class LiveQueryImpl[A](
                                       query: String
                                    )(implicit system: ActorSystem) extends LiveQuery[A] {

  import collection.JavaConverters._
  def execute(args: Object*)(implicit db: ODatabaseDocumentTx): CancellablePublisher[A] = {
    val actorRef = system.actorOf(Props(new ActorSource[Object]))

    val listener = new OLiveResultListener {
      override def onLiveResult(iLiveToken: Int, iOp: ORecordOperation): Unit = {
        println("live token!", iLiveToken, iOp)
        actorRef ! iOp
      }
    }

    import com.orientechnologies.orient.core.sql.query.OResultSet
    val q:OResultSet[ODocument] = db.query(new OLiveQuery[A](query, listener)) // todo!
    val token: java.lang.Integer = (q.get(0).field("token"))
    println("TOKEN FOUND", token)

    CancellablePublisher(actor.ActorPublisher[A](actorRef), 1234)
  }

}
