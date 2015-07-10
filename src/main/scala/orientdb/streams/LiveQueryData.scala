package orientdb.streams

import com.orientechnologies.orient.core.record.impl.ODocument

sealed trait LiveQueryData

final case class Loaded(data: ODocument) extends LiveQueryData
final case class Updated(data: ODocument) extends LiveQueryData
final case class Deleted(data: ODocument) extends LiveQueryData
final case class Created(data: ODocument) extends LiveQueryData

final case class LiveQueryDataWithToken(data: LiveQueryData, token: Int)
