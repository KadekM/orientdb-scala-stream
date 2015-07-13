package orientdb.stream

// Inspired by akka internal api
object OverflowStrategy {
  sealed trait OverflowStrategy

  /**
   * If the buffer is full, drop oldest element
   * x ~> [b u f f e r] becomes [u f f e r x]
   */
  object DropHead extends OverflowStrategy

  /**
   * If the buffer is full, drop youngest element
   * x ~> [b u f f e r] becomes [b u f f e x]
   */
  object DropTail extends OverflowStrategy

  /**
   * If the buffer is full, drop whole buffer
   * x ~> [b u f f e r] becomes [x]
   */
  object DropBuffer extends OverflowStrategy

  /**
   * If the buffer is full, drop new element
   * x ~> [b u f f e r] becomes [b u f f e r]
   */
  object DropNew extends OverflowStrategy

  /**
   * If the buffer is full, fail the stream
   * x ~> [b u f f e r] emits BufferOverflowException
   */
  object Fail extends OverflowStrategy

  final case class BufferOverflowException(msg: String) extends RuntimeException(msg)
}
