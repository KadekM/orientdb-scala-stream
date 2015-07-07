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
  override def defaultTimeoutMillis(): Long = 100L
}) with TestNGSuiteLike with TestKitBase {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A]

  implicit lazy val system = ActorSystem()
  val uuid = java.util.UUID.randomUUID.toString
  implicit val db = new ODatabaseDocumentTx(s"memory:testdb$uuid")
  implicit val ec = system.dispatcher
  db.create()

  // big TODO... run for all impl, cleanup, etc...
  val users = (for (i ← 0 to 1000) yield {
    val doc = new ODocument("Person")
    doc.field("name", s"Luke$i")
    doc.field("surname", s"Skywalker$i")
    doc.save()
  }).toVector
  //
  override def createPublisher(elements: Long): Publisher[ODocument] = {
    val query = NonBlockingQuery[ODocument](s"SELECT * FROM Person ORDER BY name LIMIT $elements")
    query.execute()
  }

  override def createFailedPublisher(): Publisher[ODocument] = {
    val query = NonBlockingQuery[ODocument](s"SEL * FRM Person ORDER BY name")
    query.execute()
  }
}

class TckTestLocking extends TckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryLocking[A](query)
}
/*
// TODO: later
class TckTestBuffering extends TckTest {
  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](query)
}
*/
