package orientdb.streams.impl

import akka.actor.{ActorSystem, _}
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.util.Timeout
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.ActorSource.ErrorOccurred
import orientdb.streams.NonBlockingQuery
import orientdb.streams.impl.ActorSourceLocking.RegisterListener
import orientdb.streams.impl.ActorControlledResultListener.GiveMeListener
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag

private[streams] class NonBlockingQueryLocking[A: ClassTag](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: scala.collection.immutable.Map[Object, Object]) extends NonBlockingQuery[A] {

  override def execute(args: Any*)(implicit db: ODatabaseDocumentTx,
    system: ActorSystem,
    ec: ExecutionContext): Publisher[A] = {

    implicit val timeout = Timeout(3.seconds) // TODO timeout from outside
    val sourceRef = system.actorOf(Props(new ActorSourceLocking[A]))
    val listenerRef = system.actorOf(Props(new ActorControlledResultListener(sourceRef)))
    def handleErrorAtSource: PartialFunction[Throwable, Unit] = { case t: Throwable ⇒ sourceRef ! ErrorOccurred(t) }

    sourceRef ! RegisterListener(listenerRef) // <--- TODO race condition?
    val listenerFuture = (listenerRef ? GiveMeListener).mapTo[BlockingOCommandResultListener]
    listenerFuture.map { listener ⇒
      db.activateOnCurrentThread()
      //TODO: SmartOSQLNonBlockingQuery starts a new future, so we kinda have redundancy (and we need to activate db twice...)
      val oQuery = SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments, listener)
      val future: scala.concurrent.Future[Unit] = db.command(oQuery).execute(args)
      future.onFailure(handleErrorAtSource)
    }.onFailure(handleErrorAtSource)

    ActorPublisher[A](sourceRef)
  }
}
