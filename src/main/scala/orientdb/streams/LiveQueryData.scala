package orientdb.streams

import com.orientechnologies.orient.core.db.record.ORecordOperation
import com.orientechnologies.orient.core.record.ORecord

sealed trait LiveQueryData

object LiveQueryData {
  def apply(iOp: ORecordOperation): LiveQueryData = iOp.`type` match {
    case 0 => Loaded(iOp.getRecord)
    case 1 => Updated(iOp.getRecord)
    case 2 => Deleted(iOp.getRecord)
    case 3 => Created(iOp.getRecord)
  }
}

final case class Loaded(data: ORecord) extends LiveQueryData
final case class Updated(data: ORecord) extends LiveQueryData
final case class Deleted(data: ORecord) extends LiveQueryData
final case class Created(data: ORecord) extends LiveQueryData

final case class LiveQueryDataWithToken(data: LiveQueryData, token: Int)
