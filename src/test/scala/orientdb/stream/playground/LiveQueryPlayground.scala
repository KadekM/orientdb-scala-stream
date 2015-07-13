package orientdb.stream.playground

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.ORecordOperation
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.{OLocalLiveResultListener, OLiveResultListener, OLiveQuery}
import com.orientechnologies.orient.core.sql.{OCommandSQL, OLiveCommandExecutorSQLFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import orientdb.stream.{OrientLoaderDeserializing, OverflowStrategy, LiveQuery}

class LiveQueryPlayground(_system: ActorSystem) extends TestKit(_system)
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("test"))


  //OLiveCommandExecutorSQLFactory.init()
  implicit val db = new ODatabaseDocumentTx(s"remote:localhost/test"); db.open("root", "test")
  //implicit val db = new ODatabaseDocumentTx(s"memory:mylittletest"); db.create()
  //db.activateOnCurrentThread()
  implicit val loader = OrientLoaderDeserializing();
  //db.registerHook(new OLiveQueryHook(db))
  // db.open("root", "test")


  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
   // db.drop()
  }

  val users = (for (i ← 0 to 20) yield addUser(i)).toVector

  def addUser(id: Int) = {
    val doc = new ODocument("Person")
    doc.field("name", s"Luke$id")
    doc.field("surname", s"Skywalker$id")
    doc.save()
  }

  implicit val materializer = ActorMaterializer()

  class ActorSink extends Actor with ActorSubscriber {
    import akka.stream.actor.ActorSubscriberMessage._
    var count = 1L
    def receive = {
      case OnNext(t) =>
        println(t)
        count += 1
        if (count == 3) {
          println("canceling")
         cancel()
        }
    }

    override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  // tests are TODO, naming and all
  "LiveQueryPlayground" should {
    "old" ignore {
      val listener = new OLiveResultListener {
        override def onLiveResult(iLiveToken: Int, iOp: ORecordOperation): Unit = {
          if (!db.isActiveOnCurrentThread) db.activateOnCurrentThread
         // println(iOp, Thread.currentThread.getId)
        }
      }

      db.query(new OLiveQuery[ODocument]("LIVE SELECT FROM Person",listener))

      for (i <- 1 to 2) {
        println("inserting")

        db.command(new OCommandSQL("insert into Person set name = 'foo', surname = 'bar'")).execute()

        Thread.sleep(1000)
      }
    }

    "playground, to be removed" in {
      val query = LiveQuery(0, OverflowStrategy.DropNew)("LIVE SELECT FROM Person")
      val qe = query.execute()
      val actorSink = system.actorOf(Props(new ActorSink))
     Source(qe).runWith(Sink(ActorSubscriber(actorSink)))
     // val z =Source(qe).runForeach(println)
     // z.onFailure{case e => println(e)}

      for (i <- 1 to 4) {
        println("inserting")
        db.command(new OCommandSQL("insert into Person set name = 'foo', surname = 'bar'")).execute()
        Thread.sleep(1000)
      }

      Thread.sleep(3000)
    }

    "inserting after live select" in {
    }

    "updating after live select" in {
    }

    "deleting after live select" in {
    }

    "cancelling subscription stops listening to insertions" in {
    }

    "cancelling subscription stops listening to updates" in {
    }

    "cancelling subscription stops listening to deletes" in {
    }

    "error propagation for invalid query" in {
    }

    "error propagation when database closes in middle?" in {
    }
  }
}
