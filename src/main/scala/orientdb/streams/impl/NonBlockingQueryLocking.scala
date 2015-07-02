package orientdb.streams.impl

import java.util.concurrent._

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.NonBlockingQuery
import orientdb.streams.ActorSource.{ErrorOccurred, Complete, Enqueue}
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.concurrent.blocking

/*
 This is very experimental, and generally work in progress.
 */
private[streams] class NonBlockingQueryLocking[A: ClassTag](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: scala.collection.immutable.Map[Object, Object]) extends NonBlockingQuery[A] {

  override def execute(args: Any*)(implicit db: ODatabaseDocumentTx,
    system: ActorSystem,
    ec: ExecutionContext): Publisher[A] = {

    val semaphore = new Semaphore(0)
    val actorRef = system.actorOf(ActorSourceLocking.props[A](semaphore))
    val listener = new BlockingOCommandResultListener(actorRef, semaphore)

    val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)

    val future: scala.concurrent.Future[Unit] = db.command(oQuery).execute(args)
    future.onFailure { case t: Throwable ⇒ actorRef ! ErrorOccurred(t) }

    ActorPublisher[A](actorRef)
  }
}

private[impl] class BlockingOCommandResultListener(ref: ActorRef, semaphore: Semaphore) extends OCommandResultListener {
    override def result(iRecord: Any): Boolean = blocking {
      semaphore.acquire()
      ref ! Enqueue(iRecord)
      true
    }

    override def end(): Unit = {
      ref ! Complete
    }
}
