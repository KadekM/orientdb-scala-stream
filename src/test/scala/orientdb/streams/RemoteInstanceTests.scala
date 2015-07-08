package orientdb.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.{OSQLAsynchQuery, OSQLSynchQuery}
import org.scalatest.{WordSpecLike, Matchers}
import orientdb.streams.wrappers.SmartOSQLNonBlockingQuery

class RemoteInstanceTests(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers {
  def this() = this(ActorSystem("remote-instance-tests"))
  val uuid = java.util.UUID.randomUUID.toString
  implicit val db = new ODatabaseDocumentTx(s"remote:localhost/test");db.open("root", "test")
  //implicit val db = new ODatabaseDocumentTx(s"memory:test$uuid");db.create()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher


  val amountOfRecords = 10
   /*val users = (for (i ← 0 to amountOfRecords) yield {
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

  "something" should {
    "old" ignore {

      val q = db.command(new OSQLAsynchQuery[ODocument]("SELECT * FROM Person ORDER BY name", oldStyleListener))
        .execute()
    }

    "somewhat" in {
     /* db.command(SmartOSQLNonBlockingQuery("SELECT * FROM Person ORDER BY name")).execute()


      Thread.sleep(1000)
*/
      val query = NonBlockingQueryLocking[ODocument]("SELECT * FROM Person ORDER BY name")

      Thread.sleep(1000)
      println("starting")
      Source(query.execute()).runForeach(println)


      Thread.sleep(3000)
    }
  }
}
