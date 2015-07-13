package orientdb.stream.impl

import java.util.concurrent.atomic.{ AtomicLong, AtomicBoolean }

import akka.actor.ActorRef
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.record.impl.ODocument
import orientdb.stream.OrientLoader
import scala.concurrent.blocking
import ActorSource.Enqueue

/*
 * OCommandResultListener that talks to ActorPublisher. The Actor handle ActorSource messages(events).
 * This listener acquires semaphore before sending message to actor (thus is blocking).
 * Semaphore has to be released from owners of instance of this listener - to let him emit message
 * and process another row.
 *
 * Sends messages to sourceRef when reads next record. Never sends Complete() [read end()]
 */
private[stream] class BlockingOCommandResultListener[A](sourceRef: ActorRef,
    signals: AtomicLong)(implicit loader: OrientLoader) extends OCommandResultListener {
  // shared among two threads
  private val fetchMore = new AtomicBoolean(true)

  // this is called by actor thread from outside
  def finishFetching() = {
    fetchMore.set(false)

    // let all through, completion is over...
    // release arbitrary big number (just for safety)
    signals.synchronized {
      signals.set(65536)
      signals.notifyAll()
    }
  }

  def isFinished = !fetchMore.get()

  // this is called by db thread
  override def result(iRecord: Any): Boolean = blocking {
    if (fetchMore.get()) {
      signals.synchronized {
        while (signals.get() <= 0)
          signals.wait()
        signals.decrementAndGet()
      } // this blocks until demand, and fetchMore might have been changed in meantime

      if (fetchMore.get()) {
        val document = iRecord.asInstanceOf[ODocument]
        loader(document)
        sourceRef ! Enqueue(document)
        true
      } else false
    } else false
  }

  /* Do not send OnComplete here!
   * end is called in 2 ways -
   * 1) if stream is exhausted
   * 2) if exception occurs (such as invalid query)
   * This would mean that sourceRef would receive Completed event, which would be in
   * race with Error coming from Future.
   * => sometimes you'd get Completed event even when problem occurred with your stream
   */
  override def end(): Unit = {
    fetchMore.set(false)
  }
}
