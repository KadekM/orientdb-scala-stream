package orientdb.streams.impl

import java.util.concurrent.Semaphore

import akka.actor.ActorRef
import com.orientechnologies.orient.core.command.OCommandResultListener
import scala.concurrent.blocking
import orientdb.streams.ActorSource.{Complete, Enqueue}

/*
OCommandResultListener that talks to ActorPublisher. The Actor handle ActorSource messages(events).
This listener acquires semaphore before sending message to actor (thus is blocking).
Semaphore has to be released from owners of instance of this listener - to let him emit message
and process another row.
 */
private[impl] class BlockingOCommandResultListener(sourceRef: ActorRef, semaphore: Semaphore) extends OCommandResultListener {
    override def result(iRecord: Any): Boolean = blocking {
      semaphore.acquire()
      sourceRef ! Enqueue(iRecord)
      true
    }

    override def end(): Unit = {
      sourceRef ! Complete
    }
}
