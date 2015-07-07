package orientdb.streams.impl

import akka.actor.FSM
import akka.stream.actor.ActorPublisher
import orientdb.streams.ActorSource
import orientdb.streams.ActorSource._

import scala.reflect.ClassTag

// todo: mechanism if it gets too big...
private[impl] class ActorSourceBuffering[A: ClassTag] extends FSM[State, Data] with ActorPublisher[A] {
  import akka.stream.actor.ActorPublisherMessage._

  startWith(Ready, Queue(List.empty[A]))

  when(Ready) {
    case Event(Enqueue(x: A), queue: Queue[A]) ⇒
      if (totalDemand <= 0) stay using Queue[A](queue.xs :+ x)
      else {
        onNext(x)
        stay
      }

    case Event(Cancel, _) =>
      onCompleteThenStop()
      stay

    case Event(Complete, queue: Queue[A]) ⇒
      if (queue.xs.isEmpty) onCompleteThenStop()
      goto(Completed)

    case Event(Request(demand), queue: Queue[A]) ⇒
      if (demand > queue.xs.length) {
        queue.xs.foreach(onNext)
        stay using Queue[A](List.empty[A])
      } else {
        val (send, rest) = queue.xs.splitAt(demand.toInt)
        send.foreach(onNext)
        stay using Queue[A](rest)
      }

    case Event(ErrorOccurred(t), _) ⇒
      onErrorThenStop(t)
      stay
  }

  when(Completed) {
    case Event(Request(demand), queue: Queue[A]) ⇒
      if (demand >= queue.xs.length) {
        queue.xs.foreach(onNext)
        onCompleteThenStop()
        stay
      } else {
        val (send, rest) = queue.xs.splitAt(demand.toInt)
        send.foreach(onNext)
        stay using Queue[A](rest)
      }
    case Event(Cancel, _) =>
      onCompleteThenStop()
      stay
    case Event(ErrorOccurred(t), _) =>
      t.printStackTrace()
      stay
  }
}


