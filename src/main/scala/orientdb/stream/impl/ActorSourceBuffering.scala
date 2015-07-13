package orientdb.stream.impl

import akka.actor.FSM
import akka.stream.actor.ActorPublisher
import ActorSource._
import orientdb.stream.OverflowStrategy

import scala.reflect.ClassTag
import OverflowStrategy._

private[stream] class ActorSourceBuffering[A: ClassTag](bufferSize: Int, overflowStrategy: OverflowStrategy)
    extends FSM[State, Data] with ActorPublisher[A] {
  import akka.stream.actor.ActorPublisherMessage._

  startWith(Ready, Queue(Vector.empty[A]))

  when(Ready) {
    case Event(Enqueue(x: A), queue: Queue[A]) ⇒
      if (totalDemand <= 0) {
        if (queue.xs.length < bufferSize) stay using Queue[A](queue.xs :+ x)
        else { // overflow strategies come to play
          overflowStrategy match {
            case DropHead   ⇒ stay using Queue[A](queue.xs.tail :+ x)
            case DropTail   ⇒ stay using Queue[A](queue.xs.dropRight(1) :+ x)
            case DropBuffer ⇒ stay using Queue[A](Vector[A](x))
            case DropNew    ⇒ stay
            case Fail       ⇒
              onErrorThenStop(new BufferOverflowException(s"Buffer of size $bufferSize has overflown"))
              stay using Queue[A](Vector.empty[A])
          }
        }
      } else {
        onNext(x)
        stay
      }

    case Event(Cancel, _) ⇒
      onCompleteThenStop()
      stay

    case Event(Complete, queue: Queue[A]) ⇒
      if (queue.xs.isEmpty) onCompleteThenStop()
      goto(Completed)

    case Event(Request(demand), queue: Queue[A]) ⇒
      if (demand > queue.xs.length) {
        queue.xs.foreach(onNext)
        stay using Queue[A](Vector.empty[A])
      } else {
        val (send, rest) = queue.xs.splitAt(demand.toInt)
        send.foreach(onNext)
        stay using Queue[A](rest)
      }

    case Event(ErrorOccurred(t), _) ⇒
      onErrorThenStop(t)
      stay using Queue[A](Vector.empty[A])
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
    case Event(Cancel, _) ⇒
      onCompleteThenStop()
      stay using Queue[A](Vector.empty[A])

    case Event(ErrorOccurred(t), _) ⇒
      stay using Queue[A](Vector.empty[A])
  }
}

