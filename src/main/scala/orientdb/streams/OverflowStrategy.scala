package orientdb.streams

// Inspired by akka internal api
object OverflowStrategy {
  sealed trait OverflowStrategy

  // TODO documentation
  object DropHead extends OverflowStrategy
  object DropTail extends OverflowStrategy
  object DropBuffer extends OverflowStrategy
  object DropNew extends OverflowStrategy
  object Fail extends OverflowStrategy

  final case class BufferOverflowException(msg: String) extends RuntimeException(msg)
}
