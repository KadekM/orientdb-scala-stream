package orientdb.streams

import akka.actor.{ ActorRef, Props, ActorSystem, Actor }
import akka.actor.Actor.Receive
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.testkit.scaladsl.TestSink
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery
import com.orientechnologies.orient.server.distributed.task.OSQLCommandTask
import org.reactivestreams.Publisher
import org.scalatest.{ Matchers, WordSpecLike, BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.Matcher
import akka.testkit._

import scala.reflect.ClassTag

class AsyncQueryTest(_system: ActorSystem) extends TestKit(_system)
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("test"))
  implicit val db = new ODatabaseDocumentTx(s"memory:testdb")
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

  "AsyncQuery" should {

    "fetch correct elements and emit onComplete" in {
      val query = AsyncQuery[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 3")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.request(3)
      src.requestNext(users(0))
      src.requestNext(users(1))
      src.requestNext(users(10))
      src.expectComplete()
    }

    "complete instantly for empty collection" in {
      val query = AsyncQuery[ODocument]("SELECT * FROM Person WHERE name='foobar'")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.expectSubscription()
      src.expectComplete()
    }

    "emit error when query fails" in {
      val query = AsyncQuery[ODocument]("SELC * FROM Person")

      val src = Source(query.execute())
        .runWith(TestSink.probe[ODocument])

      src.expectSubscription()
      src.expectError()
    }
  }
}
