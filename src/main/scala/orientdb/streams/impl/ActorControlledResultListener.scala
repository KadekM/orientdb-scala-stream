package orientdb.streams.impl

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorRef }
import orientdb.streams.impl.ActorControlledResultListener.{Finish, GiveMeListener, TotalDemandUpdate}

private object ActorControlledResultListener {
  sealed trait Message
  final case class TotalDemandUpdate(totalDemand: Long) extends Message
  case object GiveMeListener extends Message
  case object Finish extends Message
}

private class ActorControlledResultListener(sourceRef: ActorRef) extends Actor {
  val counter = new AtomicLong(0L)
  val listener = new BlockingOCommandResultListener(sourceRef, counter)

  def receive = {
    case TotalDemandUpdate(totalDemand)   ⇒
      if (totalDemand > 0) {
        counter.synchronized {
          counter.set(totalDemand)
          counter.notify()
        }
      }

    case GiveMeListener ⇒
      sender() ! listener
    case Finish =>
      listener.finishFetching()
      context.stop(self)
  }
}