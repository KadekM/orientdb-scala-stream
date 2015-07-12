package orientdb.streams.impl

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.scalatest._
import ActorSource.Enqueue
import orientdb.streams.OverflowStrategy
import orientdb.streams.OverflowStrategy.OverflowStrategy

class ActorSourceBufferingTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("actor-source-buffering-test"))
  implicit val materializer = ActorMaterializer()

  override def afterAll() = TestKit.shutdownActorSystem(system)

  def actorWithFullBuffer(overflowStrategy: OverflowStrategy, elements: Int*) = {
    val actor = system.actorOf(Props(new ActorSourceBuffering[Int](elements.size, overflowStrategy)))
    for (x <- elements) actor ! Enqueue(x)
    actor
  }

  "Buffer" should {
    "be FIFO" in {
      val ref = actorWithFullBuffer(OverflowStrategy.DropHead, 1, 2, 3)

      Source(ActorPublisher(ref)).runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .requestNext(3)
        .expectNoMsg()
    }
  }

  "DropHead" should {
    "drop oldest element" in {
      val ref = actorWithFullBuffer(OverflowStrategy.DropHead, 1, 2, 3)

      ref ! Enqueue(9)

      Source(ActorPublisher(ref)).runWith(TestSink.probe[Int])
        .requestNext(2)
        .requestNext(3)
        .requestNext(9)
        .expectNoMsg()
    }
  }

  "DropTail" should {
    "drop youngest element" in {
      val ref = actorWithFullBuffer(OverflowStrategy.DropTail, 1, 2, 3)

      ref ! Enqueue(9)

      Source(ActorPublisher(ref)).runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .requestNext(9)
        .expectNoMsg()
    }
  }

  "DropBuffer" should {
    "drop whole buffer" in {
      val ref = actorWithFullBuffer(OverflowStrategy.DropBuffer, 1, 2, 3)

      ref ! Enqueue(9)

      Source(ActorPublisher(ref)).runWith(TestSink.probe[Int])
        .requestNext(9)
        .expectNoMsg()
    }
  }

  "DropNew" should {
    "drop new element" in {
      val ref = actorWithFullBuffer(OverflowStrategy.DropNew, 1, 2, 3)

      ref ! Enqueue(9)

      Source(ActorPublisher(ref)).runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .requestNext(3)
        .expectNoMsg()
    }
  }

  "Fail" should {
    "fail the stream" in {
      val ref = actorWithFullBuffer(OverflowStrategy.Fail, 1, 2, 3)

      ref ! Enqueue(9)

      Source(ActorPublisher(ref)).runWith(TestSink.probe[Int])
        .expectSubscriptionAndError()
    }
  }
}
