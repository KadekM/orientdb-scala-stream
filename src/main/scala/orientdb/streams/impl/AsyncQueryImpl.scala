package orientdb.streams.impl

import akka.actor.{ ActorRef, Props, ActorSystem }
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery
import org.reactivestreams.Publisher
import orientdb.streams.AsyncQuery
import orientdb.streams.impl.ActorSource.{ Enqueue, Complete }

private[streams] class AsyncQueryImpl[A](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: scala.collection.immutable.Map[Object, Object])(implicit system: ActorSystem) extends AsyncQuery[A] {

  import collection.JavaConverters._
  def execute(args: Object*)(implicit db: ODatabaseDocumentTx): Publisher[A] = {
    val actorRef = system.actorOf(Props(new ActorSource[Object]))
    val listener = createListener(actorRef)
    val oQuery =
      new OSQLNonBlockingQuery[Object](query, limit, fetchPlan, arguments.asJava, listener)

    db.command(oQuery).execute(args)

    ActorPublisher[A](actorRef)
  }

  private def createListener(ref: ActorRef) = new OCommandResultListener {
    override def result(iRecord: scala.Any): Boolean = {
      ref ! Enqueue(iRecord)
      true // todo we always request all
    }

    override def end(): Unit = {
      ref ! Complete
    }
  }
}

private[streams] abstract class AsyncQueryImpl2[A]()(implicit system: ActorSystem) extends AsyncQuery[A] {
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

}
