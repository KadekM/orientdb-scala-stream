package orientdb.streams.shared

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.query.live.OLiveQueryHook
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.{ OLiveCommandExecutorSQLFactory, OCommandSQL }
import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ PublisherVerification, TestEnvironment }
import org.scalatest.testng.TestNGSuiteLike
import orientdb.streams.{ LiveQueryData, LiveQuery, NonBlockingQuery, OrientLoaderDeserializing }

import scala.reflect.ClassTag

abstract class TckTestLive extends PublisherVerification[LiveQueryData](new TestEnvironment() {
  override def defaultTimeoutMillis(): Long = 200L
}) with TestNGSuiteLike with TestKitBase {

  protected val uuid = java.util.UUID.randomUUID.toString
  protected def prepareDb(): ODatabaseDocumentTx
  protected def beforeEachPublisher(): Unit = {}
  implicit var db: ODatabaseDocumentTx = prepareDb() // TODO: ugly hack
  implicit val loader = OrientLoaderDeserializing()
  implicit lazy val system = ActorSystem()
  implicit val ec = system.dispatcher
  val maxNumberOfElementsToInsert = 20

  override def createPublisher(elements: Long): Publisher[LiveQueryData] = {
    beforeEachPublisher()
    val query = // LIMIT cant be <= 0, so we just return empty set
      if (elements <= 0) LiveQuery(s"LIVE SELECT FROM DataTable WHERE name='IDontExist'")
      else {
        LiveQuery(s"LIVE SELECT FROM DataTable")
      }
    val source = query.execute()

    // we bound it so tests dont reuqire too much (inserting Int.MaxValue-times to db would take a while)
    for (_ ← 1L to Math.min(maxNumberOfElementsToInsert, elements)) {
      db.command(new OCommandSQL("insert into DataTable set key = 'value'")).execute().asInstanceOf[ODocument]
    }
    db.commit()

    source
  }

  override def createFailedPublisher(): Publisher[LiveQueryData] = {
    LiveQuery(s"LV SL FROM DataTable").execute()
  }

  override def maxElementsFromPublisher(): Long = publisherUnableToSignalOnComplete()
}

class InMemoryTckTestLive extends TckTestLive {
  def prepareDb(): ODatabaseDocumentTx = {
    OLiveCommandExecutorSQLFactory.init()
    val db = new ODatabaseDocumentTx(s"memory:testdb$uuid")
    db.create()
    val doc = new ODocument("DataTable")
    doc.field("key", s"value")
    doc.save()
    db.registerHook(new OLiveQueryHook(db))
    db.commit()
    db
  }
}

// REQUIRES SETUP BEFORE RUN
class RemoteTckTestLive extends TckTestLive {
  override def beforeEachPublisher(): Unit = {
    if (db.isClosed) {
      db = prepareDb()
    }
  }

  def prepareDb(): ODatabaseDocumentTx = {
    val db = new ODatabaseDocumentTx(s"remote:localhost/test")
    db.open("root", "test")
    db.command(new OCommandSQL("DELETE FROM DataTable")).execute()
    val doc = new ODocument("DataTable")
    doc.field("key", s"value")
    doc.save()
    db.commit()
    db
  }
}
