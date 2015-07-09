package orientdb.streams.impl

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.ActorSource._
import orientdb.streams.NonBlockingQuery
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

private[streams] class NonBlockingQueryBuffering[A: ClassTag](query: String,
  limit: Int,
  fetchPlan: String,
  arguments: scala.collection.immutable.Map[Object, Object])
    extends NonBlockingQuery[A] {

  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext): Publisher[A] = {
    val sourceRef = system.actorOf(Props(new ActorSourceBuffering[A]))
    val listener = createListener(sourceRef)
    val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)

    val future: Future[Unit] = db.command(oQuery).execute(args: _*)
    future.onFailure { case t: Throwable ⇒ sourceRef ! ErrorOccurred(t) }
    future.onSuccess { case _ ⇒ sourceRef ! Complete }

    ActorPublisher[A](sourceRef)
  }

  private def createListener(ref: ActorRef) = new OCommandResultListener {
    override def result(iRecord: Any): Boolean = {
      ref ! Enqueue(iRecord)
      val forceFetch = iRecord.toString // TODO to be changed...
      true
    }

    override def end(): Unit = {
    }
  }
}
