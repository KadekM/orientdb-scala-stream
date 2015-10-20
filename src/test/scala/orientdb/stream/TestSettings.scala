package orientdb.stream

import com.typesafe.config.{ConfigFactory, Config}

class TestSettings(config: Config) {
  import config._
  final val memoryDb = getString("test.tck.memory-db")
  final val remoteDb = getString("test.tck.remote-db")
  final val maxElementsFromPublisher = getInt("test.tck.max-elements-from-publisher")
  final val user = getString("test.tck.user")
  final val password = getString("test.tck.password")
}

trait GotTestSettings {
  val settings = new TestSettings(ConfigFactory.load)
}
