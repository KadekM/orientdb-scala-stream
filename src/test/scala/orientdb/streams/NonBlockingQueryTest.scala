package orientdb.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.reactivestreams.Publisher
import org.reactivestreams.tck.PublisherVerification
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

      src.requestNext(users(0))
      src.requestNext(users(1))
      src.requestNext(users(10))
      src.expectComplete()
    }

    "fetch correct amount for single demand request" in {
      val query = NonBlockingQuery[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 10")

      val src = Source(query.execute()).runWith(TestSink.probe[ODocument])

      src.request(10)
      for (i <- 1 to 10) src.expectNext()
      src.expectComplete()
    }

    "not overflow and sends all elements in" ignore {
      val query = NonBlockingQuery[ODocument]("SELECT * FROM Person ORDER BY name")

      val src = Source(query.execute()).runWith(TestSink.probe[ODocument])

      src.request(Long.MaxValue)
      src.request(10000L)

      for (i <- 1 to 20) src.expectNext()
      src.expectComplete()
    }

    "complete instantly for empty collection" in {
      val query = NonBlockingQuery[ODocument]("SELECT * FROM Person WHERE name='foobar'")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.expectSubscriptionAndComplete()
    }

    "emit error when query fails" in {
      val query = NonBlockingQuery[ODocument]("SELC * FROM Person")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.expectSubscriptionAndError()
    }

    "be cancellable" in {
      val query = NonBlockingQuery[ODocument]("SELECT * FROM Person ORDER by name")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])
      src.requestNext(users(0))
      src.requestNext(users(1))
      src.requestNext(users(10))
      src.cancel()
      src.request(3)
      src.expectNoMsg()
    }
  }
}