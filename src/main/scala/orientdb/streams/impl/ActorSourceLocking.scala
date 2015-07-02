package orientdb.streams.impl

import akka.actor.ActorRef
import akka.stream.actor.ActorPublisher
import orientdb.streams.ActorSource.{ErrorOccurred, Complete, Enqueue}
import orientdb.streams.impl.ActorSourceLocking.RegisterListener
import orientdb.streams.impl.ActorControlledResultListener.{Finish, Release}

import scala.reflect.ClassTag

private object ActorSourceLocking {
  sealed trait Message
  final case class RegisterListener(listenerRef: ActorRef) extends Message
}

private class ActorSourceLocking[A: ClassTag]() extends ActorPublisher[A] {
  import akka.stream.actor.ActorPublisherMessage._

  def withListener(listenerRef: ActorRef): Receive = {
    case Request(demand)  ⇒ listenerRef ! Release
    case Enqueue(x: A)    ⇒ onNext(x)
    case Complete         ⇒
      listenerRef ! Finish
      onCompleteThenStop()

    case ErrorOccurred(t) ⇒
      listenerRef ! Finish
      onErrorThenStop(t)
  }

  def receive = {
    case RegisterListener(listenerRef: ActorRef) ⇒
      context.become(withListener(listenerRef), discardOld = true)
  }
}