package orientdb.streams.impl

import akka.actor.ActorRef
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import ActorSource.{ ErrorOccurred, Complete, Enqueue }
import orientdb.streams.impl.ActorSourceWithListener.{ FinishedRegisteringListener, RegisterListener }
import orientdb.streams.impl.ActorControlledResultListener.{ Stop, RequestedDemand }

import scala.reflect.ClassTag

private[streams] object ActorSourceWithListener {
  sealed trait Message
  final case class RegisterListener(listenerRef: ActorRef) extends Message
  case object FinishedRegisteringListener extends Message
}

private[streams] class ActorSourceWithListener[A: ClassTag]() extends ActorPublisher[A] {
  import akka.stream.actor.ActorPublisherMessage._

  def withListener(listenerRef: ActorRef): Receive = {
    case Request(demand) ⇒
      listenerRef ! RequestedDemand(demand)
    case Enqueue(x: A) ⇒
      onNext(x)

    // Complete may come in 2 ways:
    // - from BlockingOCommandResultListener, when the db stream is exhausted
    // - from downstream, when it is canceled.
    // If it's canceled, we want to tell DB to stop processing. Otherwise just stop.
    case Complete ⇒
      if (isCanceled) {
        listenerRef ! Stop
      }
      onCompleteThenStop()

    case Cancel =>
      onCompleteThenStop()

    case ErrorOccurred(t) ⇒
      listenerRef ! Stop
      //t.printStackTrace()
      onErrorThenStop(t)
  }

  def receive = {
    case RegisterListener(listenerRef: ActorRef) ⇒
      context.become(withListener(listenerRef), discardOld = true)
      sender() ! FinishedRegisteringListener
  }
}