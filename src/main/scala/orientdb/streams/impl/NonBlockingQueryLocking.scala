package orientdb.streams.impl

import java.util.concurrent.Semaphore

import akka.actor.ActorSystem
import akka.stream.actor.ActorPublisher
import akka.util.Timeout
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.ActorSource.{ Complete, Enqueue, ErrorOccurred }
import orientdb.streams.NonBlockingQuery
import orientdb.streams.impl.ListenerActor.{GiveMeListener, Release}
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

import scala.concurrent.{ Await, ExecutionContext, blocking }
import scala.reflect.ClassTag
import akka.actor._
import akka.pattern.ask

import scala.concurrent.duration._

private[streams] class NonBlockingQueryLocking[A: ClassTag](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: scala.collection.immutable.Map[Object, Object]) extends NonBlockingQuery[A] {

  override def execute(args: Any*)(implicit db: ODatabaseDocumentTx,
    system: ActorSystem,
    ec: ExecutionContext): Publisher[A] = {

    val sourceRef = system.actorOf(Props(new ActorSourceLocking[A]))
    def handleErrorAtSource: PartialFunction[Throwable, Unit] = { case t: Throwable ⇒ sourceRef ! ErrorOccurred(t) }

    val listenerRef = system.actorOf(Props(new ListenerActor(sourceRef)))
    sourceRef ! listenerRef // <--- TODO race condition?

    implicit val timeout = Timeout(3.seconds) // TODO timeout from outside
    val f = (listenerRef ? GiveMeListener).mapTo[BlockingOCommandResultListener]
    f.map { listener ⇒
      db.activateOnCurrentThread()
      //TODO: SmartOSQLNonBlockingQuery starts a new future, so we kinda have redundancy (and we need to activate db twice...)
      val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)
      val future: scala.concurrent.Future[Unit] = db.command(oQuery).execute(args)
      future.onFailure(handleErrorAtSource)
    }.onFailure(handleErrorAtSource)

    ActorPublisher[A](sourceRef)
  }
}

private class ListenerActor(sourceRef: ActorRef) extends Actor {
  val semaphore = new Semaphore(0)
  val listener = new BlockingOCommandResultListener(sourceRef, semaphore)

  override def receive = {
    case Release ⇒ semaphore.release()
    case GiveMeListener ⇒ sender() ! listener
  }
}

private object ListenerActor {
  sealed trait Message
  case object Release extends Message
  case object GiveMeListener extends Message
}

private class ActorSourceLocking[A: ClassTag]() extends ActorPublisher[A] {

  var listener: ActorRef = null // todo remove listener
  import akka.stream.actor.ActorPublisherMessage._

  override def receive = {
    // todo pimp it, with counter so we dont always need to release/lock
    case Request(demand) ⇒
      if (listener != null) {
        listener ! Release
      }
    case actor: ActorRef ⇒
      listener = actor
    case Enqueue(x: A)    ⇒ onNext(x)
    case Complete         ⇒ onCompleteThenStop()
    case ErrorOccurred(t) ⇒ onErrorThenStop(t)
  }
}
