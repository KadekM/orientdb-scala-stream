package orientdb.streams.impl

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.reactivestreams.Publisher
import orientdb.streams.ActorSource._
import orientdb.streams.{OrientLoader, NonBlockingQuery}
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

private[streams] class NonBlockingQueryBuffering[A: ClassTag](query: String,
  limit: Int,
  fetchPlan: String,
  arguments: scala.collection.immutable.Map[Object, Object])
    extends NonBlockingQuery[A] {

  def execute[B](args: AnyRef*)(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext, loader: OrientLoader[B]): Publisher[B] = {
    val sourceRef = system.actorOf(Props(new ActorSourceBuffering[A]))
    val listener = createListener(sourceRef)
    val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)

    val future: Future[Unit] = db.command(oQuery).execute(args: _*)
    future.onFailure { case t: Throwable ⇒ sourceRef ! ErrorOccurred(t) }
    future.onSuccess { case _ ⇒ sourceRef ! Complete }

    ActorPublisher[B](sourceRef)
  }

  private def createListener[B](ref: ActorRef)(implicit loader: OrientLoader[B]) = new OCommandResultListener {
    override def result(iRecord: Any): Boolean = {
      val casted = iRecord.asInstanceOf[ODocument]
      ref ! Enqueue(loader(casted))
      true
    }

    override def end(): Unit = {
    }
  }
}
