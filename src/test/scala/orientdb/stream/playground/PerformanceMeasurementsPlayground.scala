package orientdb.stream.playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{ Matchers, WordSpecLike }
import orientdb.stream._
import OverflowStrategy.Fail

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.ClassTag

// TODO no real tests, just something
abstract class PerformanceMeasurements(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with GotTestSettings {
  val uuid = java.util.UUID.randomUUID.toString
  //implicit val db = new ODatabaseDocumentTx(s"memory:testdb$uuid");db.create()
  implicit val db = new ODatabaseDocumentTx(settings.remoteDb); db.open(settings.user, settings.password)
  implicit val materializer = ActorMaterializer()
  implicit val loader = OrientLoaderDeserializing()
  implicit val ec = system.dispatcher
  val amountOfUsers = 10000

  val runtime = Runtime.getRuntime()
  def name: String
  /*

  val users = (for (i ← 0 to amountOfUsers) yield {
    val doc = new ODocument("Person")
    doc.field("name", s"Luke$i")
    doc.field("surname", s"Skywalker$i")
    doc.save()
  }).toVector
*/

  // just toy tools

  def memory[R](block: ⇒ R): R = {
    val before = runtime.totalMemory - runtime.freeMemory
    val r = block
    val after = runtime.totalMemory - runtime.freeMemory
    println(s"$name| Used memory: ${after - before}")
    r
  }

  def time[R](block: ⇒ R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println(s"$name| Elapsed time: (${t1 - t0}) ns")
    result
  }

  def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A]

  "PerformancePlayground" ignore {
    "something" in {
      time {
        val query = {
          NonBlockingQuery[ODocument]("SELECT * FROM Person ORDER BY name")
        }
        val src = Source.fromPublisher(query.execute()).map(x ⇒ 1).runFold(0)(_ + _)

        val r = Await.result(src, 100.seconds)

        println(r)

      }
    }
  }
}

class PerformanceMeasurementsBuffering(_system: ActorSystem) extends PerformanceMeasurements(_system) {
  def this() = this(ActorSystem("performance-non-blocking-query-buffering"))
  def name = "buffering"
  override def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBuffering[A](10000, Fail)(query)
}

class PerformanceMeasurementsBackpressuring(_system: ActorSystem) extends PerformanceMeasurements(_system) {
  def this() = this(ActorSystem("performance-non-blocking-query-locking"))
  def name = "locking"
  override def NonBlockingQuery[A: ClassTag](query: String): NonBlockingQuery[A] = NonBlockingQueryBackpressuring[A](query)
}
