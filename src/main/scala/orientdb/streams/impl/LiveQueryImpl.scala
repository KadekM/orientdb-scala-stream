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

import scala.reflect.ClassTag

////////////////////////////////////////////////////////
/* todo: work in progress, design will be greatly changed */
////////////////////////////////////////////////////////

trait CompleteablePublisher[A] extends Publisher[A] {
  val token: Int
  def complete()(implicit db: ODatabaseDocumentTx): Unit
}

object CompleteablePublisher {
  def apply[A](publisher: Publisher[A], token: Int): CompleteablePublisher[A] =
    new CompleteablePublisherImpl[A](publisher, token)
}

class CompleteablePublisherImpl[A](publisher: Publisher[A], val token: Int) extends CompleteablePublisher[A] {
  override def complete()(implicit db: ODatabaseDocumentTx): Unit =
    db.command(new OCommandSQL(s"live unsubscribe $token")).execute()

  override def subscribe(s: Subscriber[_ >: A]): Unit = publisher.subscribe(s)
}

private[streams] class LiveQueryImpl[A: ClassTag](
                                       query: String
                                    )(implicit system: ActorSystem) extends LiveQuery[A] {

  //todo types
  def execute(args: Any*)(implicit db: ODatabaseDocumentTx): CompleteablePublisher[A] = {
    val actorRef = system.actorOf(Props(new ActorSource[A]))

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

    CompleteablePublisher(actor.ActorPublisher[A](actorRef), 1234)
  }

}
