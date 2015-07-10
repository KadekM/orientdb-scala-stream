package orientdb.streams.playground

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit._
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.query.live.OLiveQueryHook
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.{OCommandSQL, OLiveCommandExecutorSQLFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import orientdb.streams.{LiveQuery, OrientLoaderDeserializing}

class LiveQueryPlayground(_system: ActorSystem) extends TestKit(_system)
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("test"))


  OLiveCommandExecutorSQLFactory.init()
  //implicit val db = new ODatabaseDocumentTx(s"remote:localhost/test")
  implicit val db = new ODatabaseDocumentTx(s"memory:mylittletest")
  db.activateOnCurrentThread()
  implicit val loader = OrientLoaderDeserializing()
  db.registerHook(new OLiveQueryHook(db));
  // db.open("root", "test")
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

  // tests are TODO, naming and all
  "LiveQuery (TODO, not live queries do not work in RC4)" should {
    "playground, to be removed" in {
      val query = LiveQuery("LIVE SELECT FROM Person")
      val qe = query.execute()
      val actorSink = system.actorOf(Props(new ActorSink))
      Source(qe).runWith(Sink(ActorSubscriber(actorSink)))

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
