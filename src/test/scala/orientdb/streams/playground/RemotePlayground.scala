package orientdb.streams.playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery
import org.scalatest.{Matchers, WordSpecLike}
import orientdb.streams.{OrientLoaderDeserializing, NonBlockingQueryBackpressuring}

import scala.util.Try

// just playground
class RemotePlayground(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers {
  def this() = this(ActorSystem("remote-instance-tests"))
  val uuid = java.util.UUID.randomUUID.toString
  implicit val db = new ODatabaseDocumentTx(s"remote:localhost/test"); db.open("root", "test")
  //implicit val db = new ODatabaseDocumentTx(s"memory:test$uuid");db.create()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val loader = OrientLoaderDeserializing()

  /*
  val amountOfRecords = 1000
   val users = (for (i ← 0 to amountOfRecords) yield {
      val doc = new ODocument("Person")
      doc.field("name", s"Luke$i")
      doc.field("surname", s"Skywalker$i")
      doc.save()
    }).toVector
    db.commit()*/
  /*

  if (db.countClass("Person") != amountOfRecords) {
    db.command(new OCommandSQL("DELETE * FROM Person"))
    db.commit()

    val users = (for (i ← 0 to amountOfRecords) yield {
      val doc = new ODocument("Person")
      doc.field("name", s"Luke$i")
      doc.field("surname", s"Skywalker$i")
      doc.save()
    }).toVector
    db.commit()
  }
*/

  val oldStyleListener = new OCommandResultListener {
    override def result(iRecord: scala.Any): Boolean = {
      println("old", iRecord)
      true
    }
    override def end(): Unit = {}
  }

  "RemotePlayground" ignore {
    "old" ignore {

      val q = db.command(new OSQLAsynchQuery[ODocument]("SELECT * FROM Person ORDER BY name", oldStyleListener))
        .execute()
    }

    "somewhat" in {
      val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person ORDER BY name")

      println("starting")
        //.map(_.field("name"))

      Source(query.execute()).runForeach(println)
      //val src = Source(query.execute()).runWith(TestSink.probe[ODocument])
      //src.request(10000)
      Thread.sleep(1000)
    }

    "wiy" ignore {

      val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 3")
      val src = Source(query.execute()).runWith(TestSink.probe[ODocument])

      src.request(3)
      src.expectNext()
      src.expectNext()
      src.expectNext()
      src.expectComplete()
    }

    "why5" ignore {
      val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 5")
      val src = Source(query.execute()).runWith(TestSink.probe[ODocument])

      src.request(5)
      src.expectNext()
      src.expectNext()
      src.expectNext()
      src.expectNext()
      src.expectNext()
      src.expectComplete()
    }

    "error" ignore {
      val query = NonBlockingQueryBackpressuring[ODocument]("SEL * FROM Person ORDER BY name LIMIT 3")
      val src = Source(query.execute()).runWith(TestSink.probe[ODocument])

      src.expectSubscriptionAndError()
    }

  }
}
