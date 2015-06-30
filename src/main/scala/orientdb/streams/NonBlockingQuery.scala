package orientdb.streams

import akka.actor.ActorSystem
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery
import org.reactivestreams.Publisher
import orientdb.streams.impl._

import scala.reflect.ClassTag

trait NonBlockingQuery[A] {
  def execute(args: Any*)(implicit db: ODatabaseDocumentTx): Publisher[A]
}

object NonBlockingQuery {
  def apply[A: ClassTag](query: String,
               limit: Int = -1,
               fetchPlan: String = null,
               args: Map[Object, Object] = Map.empty[Object, Object])
              (implicit system: ActorSystem)  =
    new NonBlockingQueryImpl[A](query, limit, fetchPlan, args)
/*
  // throws away listener
  def apply[A](cmd: OSQLNonBlockingQuery[A])(implicit system: ActorSystem): Publisher[A] =
    new AsyncQueryImpl[A](cmd.getText, cmd.getLimit, cmd.getFetchPlan, cmd.getParameter)*/
}


