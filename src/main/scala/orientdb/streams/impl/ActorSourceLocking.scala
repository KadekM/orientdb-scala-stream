package orientdb.streams.impl

import java.util.concurrent.Semaphore

import akka.actor.{Props, Actor}
import akka.stream.actor.ActorPublisher
import orientdb.streams.ActorSource._

import scala.reflect.ClassTag

private[streams] class ActorSourceLocking[A: ClassTag](semaphore: Semaphore) extends ActorPublisher[A] {
  import akka.stream.actor.ActorPublisherMessage._

  // TODO can be optimized so that
  // 1. locking is minimized (for high demand)
  // 2. buffers little bit (but maybe he should not care?)
  override def receive: Actor.Receive = {
    case Request(demand) =>
      if (demand > 0) {
        semaphore.release()
      }

    case Enqueue(x: A) => onNext(x)
    case Complete => onCompleteThenStop()
    case ErrorOccurred(t) => onErrorThenStop(t)
  }
}

private[streams] object ActorSourceLocking {
  def props[A: ClassTag](semaphore: Semaphore) = Props(new ActorSourceLocking[A](semaphore))
}
