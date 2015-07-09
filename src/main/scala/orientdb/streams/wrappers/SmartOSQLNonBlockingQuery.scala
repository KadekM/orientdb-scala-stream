package orientdb.streams.wrappers

import java.io.InvalidClassException

import com.orientechnologies.orient.client.remote.OStorageRemoteThread
import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelegate
import com.orientechnologies.orient.core.sql.query.{ OResultSet, OSQLQuery }

import scala.concurrent.{ Future, ExecutionContext }
import scala.util.{ Success, Try }

object SmartOSQLNonBlockingQuery {
  OCommandManager.instance().registerExecutor(classOf[SmartOSQLNonBlockingQuery[_]], classOf[OCommandExecutorSQLDelegate])

  import collection.JavaConverters._
  def apply[A](query: String,
    limit: Int,
    fetchPlan: String,
    arguments: Map[Object, Object],
    listener: OCommandResultListener)(implicit ec: ExecutionContext): OSQLQuery[A] = new SmartOSQLNonBlockingQuery[A](query, limit, fetchPlan, arguments.asJava, listener)
}
// This class's execute reflect OSQLNonBlockingQuery's execute, except uses Scala's Future and
// correctly propagates problems (future fails).
private[wrappers] class SmartOSQLNonBlockingQuery[A](query: String)(implicit ec: ExecutionContext)
    extends OSQLQuery[A](query) with OCommandRequestAsynch {

  def this(query: String,
    limit: Int,
    fetchPlan: String,
    arguments: java.util.Map[Object, Object],
    listener: OCommandResultListener)(implicit ec: ExecutionContext) = {
    this(query)
    this.fetchPlan = fetchPlan
    this.limit = limit
    this.resultListener = listener
    this.parameters = arguments
  }

  override def isAsynchronous: Boolean = true

  override def execute[RET](iArgs: AnyRef*): RET = {

    val database = ODatabaseRecordThreadLocal.INSTANCE.get()
    val future = database match {
      case tx: ODatabaseDocumentTx ⇒
        Future {
          val db = tx.copy() // copy must be inside future's closure! it does more than just copy
          try { // TODO: can be done nicer... maybe once we merge two futures (callers and this)
            val value: OResultSet[_] = superExecute(iArgs: _*) // scala compiler bug I assume
          } catch { case t: Throwable =>
            db.close()
            throw t
          }
        }
      case _ ⇒ Future.failed(new InvalidClassException("database is not of type ODatabaseDocumentTx"))
    }

    future.asInstanceOf[RET]
  }

  private def superExecute[RET](iArgs: AnyRef*): RET = {
    super.execute(iArgs :_*)
  }
}
