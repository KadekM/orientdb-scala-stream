package orientdb.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.reflect.ClassTag

abstract class NonBlockingQueryTest(_system: ActorSystem) extends TestKit(_system)
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A]

  val uuid = java.util.UUID.randomUUID.toString
  implicit val db = new ODatabaseDocumentTx(s"memory:testdb$uuid")
  implicit val ec = system.dispatcher
  db.create()

  val users = (for (i ← 0 to 20) yield {
    val doc = new ODocument("Person")
    doc.field("name", s"Luke$i")
    doc.field("surname", s"Skywalker$i")
    doc.save()
  }).toVector

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
    db.drop()
  }
  implicit val materializer = ActorMaterializer()

  "NonBlockingQuery" should {

    "fetch correct elements and emit onComplete" in {
      val query = NonBlockingQuery[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 3")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.request(3)
      src.requestNext(users(0))
      src.requestNext(users(1))
      src.requestNext(users(10))
      src.expectComplete()
    }

    "complete instantly for empty collection" in {
      val query = NonBlockingQuery[ODocument]("SELECT * FROM Person WHERE name='foobar'")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.expectSubscription()
      src.expectComplete()
    }

    "emit error when query fails" in {
      val query = NonBlockingQuery[ODocument]("SELC * FROM Person")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.expectSubscription()
      src.expectError()
    }
  }
}