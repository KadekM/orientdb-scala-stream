package orientdb.streams.impl

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorRef }
import orientdb.streams.impl.ActorControlledResultListener.{Finish, GiveMeListener, RequestedDemand}

private object ActorControlledResultListener {
  sealed trait Message
  final case class RequestedDemand(totalDemand: Long) extends Message
  case object GiveMeListener extends Message
  case object Finish extends Message
}

private class ActorControlledResultListener(sourceRef: ActorRef) extends Actor {
  val signals = new AtomicLong(0L)
  val listener = new BlockingOCommandResultListener(sourceRef, signals)

  def receive = {
    case RequestedDemand(demand)   ⇒
      if (demand > 0) {
        signals.synchronized {
          signals.addAndGet(demand)
          signals.notify()
        }
      }

    case GiveMeListener ⇒
      sender() ! listener
    case Finish =>
      listener.finishFetching()
      context.stop(self)
  }
}