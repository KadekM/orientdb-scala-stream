package orientdb.streams

import com.orientechnologies.orient.core.record.impl.ODocument

// todo: parent of orientdbs' stuff, generalize?
trait OrientLoader extends ((ODocument) ⇒ Unit)

private class OrientLoaderDeserializing extends OrientLoader {
  override def apply(x: ODocument): Unit = x.deserializeFields()
}
object OrientLoaderDeserializing {
  def apply(): OrientLoader = new OrientLoaderDeserializing
}
// TODO: deserializers only for some fields
