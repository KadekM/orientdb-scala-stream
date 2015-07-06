package orientdb.streams.impl

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean}

import akka.actor.ActorRef
import com.orientechnologies.orient.core.command.OCommandResultListener
import scala.concurrent.blocking
import orientdb.streams.ActorSource.{ Complete, Enqueue }

/*
OCommandResultListener that talks to ActorPublisher. The Actor handle ActorSource messages(events).
This listener acquires semaphore before sending message to actor (thus is blocking).
Semaphore has to be released from owners of instance of this listener - to let him emit message
and process another row.
 */
private[impl] class BlockingOCommandResultListener(sourceRef: ActorRef,
                                                   semaphore: BigSemaphore) extends OCommandResultListener {
  // shared among two threads
  private val fetchMore = new AtomicBoolean(true)

  // this is called by actor thread
  def finishFetching() = {
    fetchMore.set(false)

    // let all through, completion is over...
    // release arbitrary big number (just for safety)
    semaphore.drainPermits()
    semaphore.release(65536)
  }

  def isFinished = !fetchMore.get()

  // this is called by db thread
  override def result(iRecord: Any): Boolean = blocking {
    semaphore.acquire()

    sourceRef ! Enqueue(iRecord)

    fetchMore.get()
  }

  override def end(): Unit = {
    sourceRef ! Complete
  }
}
