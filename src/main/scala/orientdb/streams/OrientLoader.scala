package orientdb.streams

import com.orientechnologies.orient.core.record.impl.ODocument

// todo: parent of orientdbs' stuff, generalize?
trait OrientLoader extends ((ODocument) ⇒ Unit)

private class OrientNonLazyLoader extends OrientLoader {
  override def apply(x: ODocument): Unit = x.setLazyLoad(false)
}
object OrientNonLazyLoader {
  def apply(): OrientLoader = new OrientNonLazyLoader
}
