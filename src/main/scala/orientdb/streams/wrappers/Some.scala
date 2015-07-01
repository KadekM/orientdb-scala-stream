package orientdb.streams.wrappers

import com.orientechnologies.orient.core.command._
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLQuery

import scala.concurrent.Promise
import scala.util.Success

// This class's execute reflect OSQLNonBlockingQuery's execute, except hook the required future.
// Execution returns Future of promise. Original OSQLNonBlockingQuery returns their implementation
// of Java future.
class SmartOSQLNonBlockingQuery[A](private val query: String)
    extends OSQLQuery[A](query) with OCommandRequestAsynch {

  def this(query: String,
    limit: Int,
    fetchPlan: String,
    arguments: java.util.Map[Object, Object],
    listener: OCommandResultListener) = {
    this(query)
    this.fetchPlan = fetchPlan
    this.limit = limit
    this.resultListener = listener
    this.parameters = arguments
  }

  override def isAsynchronous: Boolean = true

  override def execute[RET](iArgs: AnyRef*): RET = {
    val database = ODatabaseRecordThreadLocal.INSTANCE.get()
    val promise = Promise[Unit]()

    if (database.isInstanceOf[ODatabaseDocumentTx]) {
      val thread = new Thread(new Runnable {
        override def run(): Unit = {
          val db = database.asInstanceOf[ODatabaseDocumentTx].copy()
          // try SmartOSQLNonBlockingQuery.super.execute(iArgs: _*) // TODO WTF? why doesnt the above work
          try superExecute(iArgs)
          catch { case e: Exception ⇒ promise.tryFailure(e) }
          finally {
            try if (db != null) db.close()
            catch { case e: Exception ⇒ promise.tryFailure(e) }
            try {
              promise.synchronized {
                promise.tryComplete(Success(Unit))
              }
            } catch { case e: Exception ⇒ promise.tryFailure(e) }
          }
        }
      })

      thread.start()
    }

    promise.future.asInstanceOf[RET]
  }

  private def superExecute[RET](iArgs: AnyRef*): RET = super.execute(iArgs: _*)
}
