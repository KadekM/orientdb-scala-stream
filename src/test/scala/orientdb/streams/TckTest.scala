package orientdb.streams

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.reactivestreams.Publisher
import org.reactivestreams.tck.{ TestEnvironment, PublisherVerification }
import org.scalatest.testng.TestNGSuiteLike

import scala.reflect.ClassTag

abstract class TckTest extends PublisherVerification[ODocument](new TestEnvironment() {
  override def defaultTimeoutMillis(): Long = 200L
}) with TestNGSuiteLike with TestKitBase {

  val uuid = java.util.UUID.randomUUID.toString
  def prepareDb(): ODatabaseDocumentTx
  implicit val db: ODatabaseDocumentTx = prepareDb()

  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A]

  implicit lazy val system = ActorSystem()
  implicit val ec = system.dispatcher

  // TODO: maxElements!
  override def createPublisher(elements: Long): Publisher[ODocument] = {
    val query = NonBlockingQuery[ODocument](s"SELECT * FROM Person ORDER BY name LIMIT $elements")
    query.execute()
  }

  override def createFailedPublisher(): Publisher[ODocument] = {
    val query = NonBlockingQuery[ODocument](s"SEL * FRM Person ORDER BY name")
    query.execute()
  }
}

abstract class InMemoryTckTest extends TckTest {
  def prepareDb(): ODatabaseDocumentTx = {
    val db = new ODatabaseDocumentTx(s"memory:testdb$uuid")
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

abstract class RemoteTckTest extends TckTest {
  def prepareDb(): ODatabaseDocumentTx = {
    // REQUIRES SETUP BEFORE RUN
    val db = new ODatabaseDocumentTx(s"remote:localhost/test")
    db.open("root", "test")
    db
  }
}

class TckTestLocalLocking extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryLocking[A](query)
}

/*
class TckTestLocalBuffering extends InMemoryTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)
}
*/

class TckTestRemoteLocking extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryLocking[A](query)
}
/*
class TckTestRemoteBuffering extends RemoteTckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)
}*/

