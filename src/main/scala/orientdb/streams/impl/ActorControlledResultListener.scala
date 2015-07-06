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
  val semaphore = new BigSemaphore()
  val listener = new BlockingOCommandResultListener(sourceRef, semaphore)

  def receive = {
    case RequestedDemand(demand)   ⇒
      if (demand > 0) {
        semaphore.release(demand)
      }

    case GiveMeListener ⇒
      sender() ! listener
    case Finish =>
      listener.finishFetching()
      context.stop(self)
  }
}