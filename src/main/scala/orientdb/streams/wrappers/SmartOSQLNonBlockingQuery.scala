package orientdb.streams.wrappers

import java.io.InvalidClassException

import com.orientechnologies.orient.client.remote.OStorageRemoteThread
import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelegate
import com.orientechnologies.orient.core.sql.query.{OResultSet, OSQLQuery}

import scala.concurrent.{ Future, ExecutionContext }

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
          //println("!!!!!!!!!!!!!!!!!!!!", tx.getStorage.asInstanceOf[OStorageRemoteThread])
          val db = tx.copy()
          //val value: OResultSet[_] = try { superEx() }
          val value: OResultSet[_] = superEx()
          /*catch{
            case e =>
              println("--------------------")
              e.printStackTrace()
              throw e
          }*/
        /*  finally {
          //  if (db != null) db.close() // TODOOOOOOOOOOOOOOO
          }*/
        }
      case _ ⇒ Future.failed(new InvalidClassException("database is not of type ODatabaseDocumentTx"))
    }

    future.asInstanceOf[RET]
  }

  private def superEx[RET](): RET = super.execute()
  private def superExecute[RET](iArgs: AnyRef*): RET = super.execute(iArgs: _*)
}
