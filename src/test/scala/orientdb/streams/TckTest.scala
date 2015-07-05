package orientdb.streams

import com.orientechnologies.orient.core.record.impl.ODocument
import org.reactivestreams.Publisher
import org.reactivestreams.tck.{TestEnvironment, PublisherVerification}
import org.scalatest.testng.TestNGSuiteLike


class TckTest extends PublisherVerification[ODocument](new TestEnvironment()) with TestNGSuiteLike{
  override def createPublisher(elements: Long): Publisher[ODocument] = ???

  override def createFailedPublisher(): Publisher[ODocument] = ???
}
