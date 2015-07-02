package orientdb.streams.impl

import java.util.concurrent.Semaphore

import akka.actor.{ Actor, ActorRef }
import orientdb.streams.impl.ResultListenerActor.{ GiveMeListener, Release }

private object ResultListenerActor {
  sealed trait Message
  case object Release extends Message
  case object GiveMeListener extends Message
}

private class ResultListenerActor(sourceRef: ActorRef) extends Actor {
  val semaphore = new Semaphore(0)
  val listener = new BlockingOCommandResultListener(sourceRef, semaphore)

  def receive = {
    case Release        ⇒ semaphore.release()
    case GiveMeListener ⇒ sender() ! listener
  }
}