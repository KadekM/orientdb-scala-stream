package orientdb.streams

import com.orientechnologies.orient.core.record.impl.ODocument

// todo: parent of orientdbs' stuff, generalize?
trait OrientLoader[A] extends ((ODocument) ⇒ A)

object OrientLoaderDeserializing {
  def apply(fields: String*): OrientLoader[ODocument] = new OrientLoader[ODocument] {
    override def apply(v1: ODocument): ODocument = {
      v1.deserializeFields(fields: _*)
      v1
    }
  }
}

object OrientLoaderIdentity {
  def apply(): OrientLoader[ODocument] = new OrientLoader[ODocument] {
    override def apply(v1: ODocument): ODocument = v1
  }
}
