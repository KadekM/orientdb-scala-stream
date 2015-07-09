package orientdb.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.ORecordOperation
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.serialization.serializer.record.binary.ORecordSerializerBinary
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.{ OResultSet, OLiveQuery, OLiveResultListener }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class LiveQueryTest(_system: ActorSystem) extends TestKit(_system)
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("test"))
  //implicit val db = new ODatabaseDocumentTx(s"remote:localhost/test")
  implicit val db = new ODatabaseDocumentTx(s"memory:mylittletest")
  implicit val loader = OrientNonLazyLoader()
  // db.open("admin", "admin")
  db.create()

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
    db.drop()
  }

  val users = (for (i ← 0 to 20) yield addUser(i)).toVector

  def addUser(id: Int) = {
    val doc = new ODocument("Person")
    doc.field("name", s"Luke$id")
    doc.field("surname", s"Skywalker$id")
    doc.save()
  }

  implicit val materializer = ActorMaterializer()

  // tests are TODO, naming and all
  "LiveQuery (TODO, not live queries do not work in RC4)" ignore {
    "playground, to be removed" in {
      val query = LiveQuery[ODocument]("LIVE SELECT FROM Person")
      Source(query.execute()).runForeach(println)
      Thread.sleep(1000)

      db.command(new OCommandSQL("insert into PERSON set name = 'foo', surname = 'bar'")).execute()

      val listener = new OLiveResultListener {
        override def onLiveResult(iLiveToken: Int, iOp: ORecordOperation): Unit = {
          println("live token!", iLiveToken, iOp)
        }
      }

      db.asInstanceOf[ODatabaseDocumentTx].setSerializer(new ORecordSerializerBinary)
      val document = new ODocument("Test")
      document.save()
      db.commit()

      val result: OResultSet[ODocument] = db.query(new OLiveQuery[ODocument]("live select from Test", listener))
      println(result.get(0))

      db.command(new OCommandSQL("insert into Test set name='foo', surname='bar'")).execute()
      db.commit()

      db.command(new OCommandSQL("update Test set name = 'baz' where surname = 'bar'")).execute()
      db.commit()

      Thread.sleep(2000)
    }

    "inserting after live select" in {
    }

    "updating after live select" in {
    }

    "deleting after live select" in {
    }

    "closing stream stops listening to insertions" in {
    }

    "closing stream stops listening to updates" in {
    }

    "closing stream stops listening to deletes" in {
    }

    "error propagation" in {
    }
  }
}
