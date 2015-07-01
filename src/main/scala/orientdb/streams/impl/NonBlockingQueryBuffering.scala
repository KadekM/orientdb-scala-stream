package orientdb.streams.impl

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OResultSet
import org.reactivestreams.Publisher
import orientdb.streams.NonBlockingQuery
import orientdb.streams.ActorSource._
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[streams] class NonBlockingQueryBuffering[A: ClassTag](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: scala.collection.immutable.Map[Object, Object])
  extends NonBlockingQuery[A] {

  def execute(args: Any*)(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext): Publisher[A] = {
    val actorRef = system.actorOf(Props(new ActorSourceBuffering[A]))
    val listener = createListener(actorRef)
    val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)

    val future: Future[Unit] = db.command(oQuery).execute(args)
    future.onFailure { case t: Throwable ⇒ actorRef ! ErrorOccurred(t) }

    ActorPublisher[A](actorRef)
  }

  private def createListener(ref: ActorRef) = new OCommandResultListener {
    override def result(iRecord: Any): Boolean = {
      ref ! Enqueue(iRecord)
      true // todo we always request all
    }

    override def end(): Unit = {
      ref ! Complete
    }
  }
}
