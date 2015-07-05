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
  val listener = new BlockingOCommandResultListener(sourceRef, semaphore)

  def receive = {
    case RequestAmount(amount)   ⇒
      // TODO: oh god... need something better... for now it eliminates race
      if (counter.addAndGet(amount) < 0) counter.set(Long.MaxValue) // dont overflow
      val release = Math.min(1024, counter.get()).toInt
      if (semaphore.availablePermits() < 65536) {
        counter.addAndGet(-release)
        semaphore.release(release)
      }

    case GiveMeListener ⇒
      sender() ! listener
    case Finish =>
      listener.finishFetching()
      context.stop(self)
  }
}