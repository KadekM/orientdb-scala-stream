package orientdb.stream

import akka.actor.{ActorRefFactory, ActorSystem}
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import OverflowStrategy.OverflowStrategy
import orientdb.stream.impl.{NonBlockingQueryBackpressuring, NonBlockingQueryBuffering}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

/*
 * Query which does not block, based on OrientDb NonBlockingQuery.
 */
trait NonBlockingQuery[A] {
  import scala.collection.JavaConverters._
  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, actorRefFactory: ActorRefFactory, ec: ExecutionContext, loader: OrientLoader): Publisher[A]
  def executePositional(args: String*)(implicit db: ODatabaseDocumentTx, actorRefFactory: ActorRefFactory, ec: ExecutionContext, loader: OrientLoader): Publisher[A] =
    execute(args: _*)
  def executeNamed(args: Map[String, String])(implicit db: ODatabaseDocumentTx, actorRefFactory: ActorRefFactory, ec: ExecutionContext, loader: OrientLoader): Publisher[A] =
    execute(args.asJava)
}

/*
 * Query based on OrientDb NonBlockingQuery
 * Query does not block database processing, but buffers database results on separate thread, and emits
 * them to downstream if there is demand.
 *
 * If downstream cancels, no more elements will be emitted, but database thread will finish it's run (doing the full
 * fetch, which will though not be buffered and emitted anymore)
 *
 * If actor's buffer is too large, it will explode (TODO)
 */
object NonBlockingQueryBuffering {
  def apply[A: ClassTag](bufferSize: Int, overflowStrategy: OverflowStrategy)(query: String,
    limit: Int = -1,
    fetchPlan: String = null,
    args: Map[Object, Object] = Map.empty[Object, Object]) =
    new NonBlockingQueryBuffering[A](bufferSize, overflowStrategy)(query, limit, fetchPlan, args)
}

/*
 * Query based on OrientDb NonBlockingQuery
 * Query blocks database processing unless there is demand. It also BLOCKS one thread, which is doing the
 * fetch on OrientDb side, so use with care. It may be much slower than using NonBlockingQueryBuffering.
 *
 * Has full support for cancellation - when downstream cancels, database will stop the processing.
 */
object NonBlockingQueryBackpressuring {
  def apply[A: ClassTag](query: String,
    limit: Int = -1,
    fetchPlan: String = null,
    args: Map[Object, Object] = Map.empty[Object, Object]) =
    new NonBlockingQueryBackpressuring[A](query, limit, fetchPlan, args)
}
