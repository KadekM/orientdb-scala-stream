package orientdb.streams.impl

import akka.actor._
import akka.stream.actor.ActorPublisher
import orientdb.streams.impl.ActorSourceLiveQuery.WaitingForToken
import orientdb.streams.{LiveQueryData, LiveQueryDataWithToken}
import ActorSourceLiveQuery._

object ActorSourceLiveQuery {
  sealed trait State
  case object WaitingForToken extends State
  case object Ready extends State
  case object Cancelled extends State

  sealed trait Event
  final case class Enqueue(x: LiveQueryDataWithToken) extends Event
  final case class TokenFound(x: Int) extends Event
  final case class ErrorOccurred(t: Throwable) extends Event
  // does not have Complete - never can be completed.
  // Can only be canceled (via classic ActorPublisherMessage.cancel)

  sealed trait Data
  final case class Queue(xs: Vector[LiveQueryData]) extends Data
  final case class QueueWithToken(xs: Vector[LiveQueryData], token: Int) extends Data
}

// todo: maybe add generality
private[impl] class ActorSourceLiveQuery extends FSM[State, Data] with ActorPublisher[LiveQueryData] {
  import akka.stream.actor.ActorPublisherMessage._
  startWith(WaitingForToken, Queue(Vector.empty[LiveQueryData]))

  when(WaitingForToken) {
    case Event(TokenFound(token: Int), queue: Queue) ⇒
      goto(Ready) using QueueWithToken(queue.xs, token)

    case Event(Enqueue(LiveQueryDataWithToken(data, token)), queue: Queue) ⇒
      goto(Ready) using QueueWithToken(queue.xs :+ data, token)

    case Event(ErrorOccurred(t), _) ⇒
      onErrorThenStop(t)
      stay

    case Event(Request(demand), queue: Queue) ⇒
      stay

    case Event(Cancel, _) ⇒
      // cant unsubscribe because no token yet received
      goto(Cancelled)
  }

  when(Ready) {
    case Event(TokenFound(token: Int), _) ⇒
      stay

    case Event(Enqueue(LiveQueryDataWithToken(data, token)), queue: QueueWithToken) ⇒
      if (totalDemand <= 0) stay using QueueWithToken(queue.xs :+ data, token)
      else {
        onNext(data)
        stay
      }

    case Event(Request(demand), queue: QueueWithToken) ⇒
      if (demand > queue.xs.length) {
        queue.xs.foreach(onNext)
        stay using Queue(Vector.empty[LiveQueryData])
      } else {
        val (send, rest) = queue.xs.splitAt(demand.toInt)
        send.foreach(onNext)
        stay using Queue(rest)
      }

    case Event(ErrorOccurred(t), _) ⇒
      stay

    case Event(Cancel, _) ⇒
      println("unsubscribing")
      //UNSUBSCRIBE
      stay
  }

  when(Cancelled) { // we were cancelled - cancel as soon as you get token
    case Event(TokenFound(token: Int), _) ⇒
      stay
    case Event(Enqueue(LiveQueryDataWithToken(_, token)), _) ⇒
      stay
    case Event(ErrorOccurred(t), _) ⇒
      stay
    case Event(Request(_), _) ⇒
      stay
    case Event(Cancel, _) ⇒
      stay
  }
}
