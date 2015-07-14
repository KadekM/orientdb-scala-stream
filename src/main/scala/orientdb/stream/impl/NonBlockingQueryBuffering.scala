package orientdb.stream.impl

import akka.actor.{ActorRefFactory, ActorRef, ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.reactivestreams.Publisher
import ActorSource._
import orientdb.stream.OverflowStrategy.OverflowStrategy
import orientdb.stream.wrappers.SmartOSQLNonBlockingQuery
import orientdb.stream.{OverflowStrategy, OrientLoader, NonBlockingQuery}
import orientdb.stream.OverflowStrategy.OverflowStrategy
import OverflowStrategy.OverflowStrategy

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

private[stream] class NonBlockingQueryBuffering[A: ClassTag](buffer: Int, overflowStrategy: OverflowStrategy)(query: String,
  limit: Int,
  fetchPlan: String,
  arguments: scala.collection.immutable.Map[Object, Object])
    extends NonBlockingQuery[A] {

  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, actorRefFactory: ActorRefFactory, ec: ExecutionContext, loader: OrientLoader): Publisher[A] = {
    val sourceRef = actorRefFactory.actorOf(Props(new ActorSourceBuffering[A](buffer, overflowStrategy)))
    val listener = createListener(sourceRef)
    val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)

    val future: Future[Unit] = db.command(oQuery).execute(args: _*)
    future.onFailure { case t: Throwable ⇒ sourceRef ! ErrorOccurred(t) }
    future.onSuccess { case _ ⇒ sourceRef ! Complete }

    ActorPublisher[A](sourceRef)
  }

  private def createListener(ref: ActorRef)(implicit loader: OrientLoader) = new OCommandResultListener {
    override def result(iRecord: Any): Boolean = {
      ref ! Enqueue(iRecord)
      loader(iRecord.asInstanceOf[ODocument]) // todo types
      true
    }

    override def end(): Unit = {
    }
  }
}
