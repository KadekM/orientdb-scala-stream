package orientdb.streams.impl

import akka.actor._
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.OCommandSQL
import orientdb.streams.impl.ActorSourceLiveQuery.WaitingForToken
import orientdb.streams.{ LiveQueryData, LiveQueryDataWithToken }
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
private[impl] class ActorSourceLiveQuery(db: ODatabaseDocumentTx)
    extends FSM[State, Data] with ActorPublisher[LiveQueryData] {

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
        stay using QueueWithToken(Vector.empty[LiveQueryData], queue.token)
      } else {
        val (send, rest) = queue.xs.splitAt(demand.toInt)
        send.foreach(onNext)
        stay using QueueWithToken(rest, queue.token)
      }

    case Event(ErrorOccurred(t), _) ⇒
      onErrorThenStop(t)
      stay

    case Event(Cancel, queue: QueueWithToken) ⇒
      cancelDb(queue.token)
      onCompleteThenStop()
      stay
  }

  when(Cancelled) { // we were cancelled - cancel as soon as you get token
    case Event(TokenFound(token: Int), _) ⇒
      cancelDb(token)
      onCompleteThenStop()
      stay
    case Event(Enqueue(LiveQueryDataWithToken(_, token)), _) ⇒
      cancelDb(token)
      onCompleteThenStop()
      stay

    case Event(ErrorOccurred(t), _) ⇒
      stay

    case Event(Request(_), _) ⇒
      stay

    case Event(Cancel, _) ⇒
      stay
  }

  //todo: this sucks incredibly... can we do better ?
  private def cancelDb(token: Int): Unit = {
    val dbCopy = db.copy() // TODO*: maybe we can send command instead of token and execute that?
    dbCopy.activateOnCurrentThread()
    dbCopy.command(new OCommandSQL(s"live unsubscribe ${token}")).execute() // see *TODO
    //dbCopy.close()
  }
}
