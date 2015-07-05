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
      // todo OVERFLOW
      // oh god... need something better... for now it eliminates race
      val release = Math.min(100, counter.addAndGet(amount)).toInt
      counter.addAndGet(-release)
      semaphore.release(release)

    case GiveMeListener ⇒
      sender() ! listener
    case Finish =>
      listener.finishFetching()
      context.stop(self)
  }
}