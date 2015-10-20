package orientdb.stream.shared

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import com.typesafe.config.ConfigFactory
import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ PublisherVerification, TestEnvironment }
import org.scalatest.testng.TestNGSuiteLike
import orientdb.stream.{GotTestSettings, TestSettings, OrientLoaderDeserializing, NonBlockingQuery}

import scala.reflect.ClassTag

abstract class TckTest extends PublisherVerification[ODocument](new TestEnvironment() {
  override def defaultTimeoutMillis(): Long = 600L
}) with TestNGSuiteLike with TestKitBase with GotTestSettings {

  protected val uuid = java.util.UUID.randomUUID.toString
  protected def prepareDb(): ODatabaseDocumentTx
  protected def beforeEachPublisher(): Unit = {}
  implicit var db: ODatabaseDocumentTx = prepareDb() // TODO: ugly hack
  implicit val loader = OrientLoaderDeserializing()

  override def maxElementsFromPublisher(): Long = settings.maxElementsFromPublisher

  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A]

  implicit lazy val system = ActorSystem()
  implicit val ec = system.dispatcher

  override def createPublisher(elements: Long): Publisher[ODocument] = {
    beforeEachPublisher()
    val query = // LIMIT cant be <= 0, so we just return empty set
      if (elements <= 0) NonBlockingQuery[ODocument](s"SELECT * FROM Person WHERE name='IDontExist'")
      else NonBlockingQuery[ODocument](s"SELECT * FROM Person ORDER BY name LIMIT $elements")
    query.execute()
  }

  override def createFailedPublisher(): Publisher[ODocument] = {
    beforeEachPublisher()
    val query = NonBlockingQuery[ODocument](s"SEL * FRM Person ORDER BY name")
    query.execute()
  }
}

abstract class InMemoryTckTest extends TckTest {
  def prepareDb(): ODatabaseDocumentTx = {
    val db = new ODatabaseDocumentTx(s"${settings.memoryDb}$uuid")
    db.create()
    val users = (for (i ← 0 to 1000) yield {
      val doc = new ODocument("Person")
      doc.field("name", s"Luke$i")
      doc.field("surname", s"Skywalker$i")
      doc.save()
    }).toVector

    db.commit()
    db
  }
}

// REQUIRES SETUP BEFORE RUN
abstract class RemoteTckTest extends TckTest {
  override def beforeEachPublisher(): Unit = {
    if (db.isClosed) {
      db = prepareDb()
    }
  }

  def prepareDb(): ODatabaseDocumentTx = {
    val db = new ODatabaseDocumentTx(settings.remoteDb)
    db.open(settings.user, settings.password)
    db
  }
}
