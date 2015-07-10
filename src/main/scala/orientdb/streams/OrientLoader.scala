package orientdb.streams

import com.orientechnologies.orient.core.record.impl.ODocument

// todo: parent of orientdbs' stuff, generalize?
trait OrientLoader extends ((ODocument) ⇒ Unit)

object OrientLoaderDeserializing {
  def apply(fields: String*): OrientLoader = new OrientLoader {
    override def apply(document: ODocument): Unit = document.deserializeFields(fields: _*)
  }
}

object OrientLoaderIdentity {
  def apply(fields: String*): OrientLoader = new OrientLoader {
    override def apply(document: ODocument): Unit = {}
  }
}
