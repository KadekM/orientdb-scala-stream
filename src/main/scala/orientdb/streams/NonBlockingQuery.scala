package orientdb.streams

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import org.reactivestreams.Publisher
import orientdb.streams.impl._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

/*
 * Query which does not block, based on OrientDb NonBlockingQuery.
 */
trait NonBlockingQuery[A] {
  import scala.collection.JavaConverters._
  def execute[B](args: AnyRef*)(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext, loader: OrientLoader[B]): Publisher[B]
  def executePositional[B](args: String*)(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext, loader: OrientLoader[B]): Publisher[B] =
    execute(args: _*)
  def executeNamed[B](args: Map[String, String])(implicit db: ODatabaseDocumentTx, system: ActorSystem, ec: ExecutionContext, loader: OrientLoader[B]): Publisher[B] =
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
  def apply[A: ClassTag](query: String,
    limit: Int = -1,
    fetchPlan: String = null,
    args: Map[Object, Object] = Map.empty[Object, Object])(implicit system: ActorSystem) =
    new NonBlockingQueryBuffering[A](query, limit, fetchPlan, args)
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
    args: Map[Object, Object] = Map.empty[Object, Object])(implicit system: ActorSystem) =
    new NonBlockingQueryBackpressuring[A](query, limit, fetchPlan, args)
}
