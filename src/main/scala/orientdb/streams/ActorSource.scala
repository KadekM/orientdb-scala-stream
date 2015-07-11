package orientdb.streams

object ActorSource {
  sealed trait State
  case object Ready extends State
  case object Completed extends State

  sealed trait Event
  final case class Enqueue[A](x: A) extends Event
  final case class ErrorOccurred(t: Throwable) extends Event
  case object Complete extends Event

  sealed trait Data
  final case class Queue[A](xs: Vector[A]) extends Data
}


