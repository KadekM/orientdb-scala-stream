package orientdb.streams.impl

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorRef }
import orientdb.streams.impl.ActorControlledResultListener.{Finish, GiveMeListener, RequestAmount}

private object ActorControlledResultListener {
  sealed trait Message
  final case class RequestAmount(amount: Long) extends Message
  case object GiveMeListener extends Message
  case object Finish extends Message
}

private class ActorControlledResultListener(sourceRef: ActorRef) extends Actor {
  val semaphore = new Semaphore(0)
  val counter = new AtomicLong(0L)
  val listener = new BlockingOCommandResultListener(sourceRef, semaphore, counter)

  def receive = {
    case RequestAmount(amount)   ⇒
      // todo overflow
      if (counter.addAndGet(amount) > 0) {
        semaphore.drainPermits()
        semaphore.release()
      }
    case GiveMeListener ⇒
      sender() ! listener
    case Finish =>
      listener.finishFetching()
      context.stop(self)
  }
}