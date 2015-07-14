package orientdb.stream.impl

import akka.actor.{ActorRefFactory, Props, ActorSystem}
import akka.stream.actor.ActorPublisher
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.ORecordOperation
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.{ OResultSet, OLiveQuery, OLiveResultListener }
import org.reactivestreams.Publisher
import orientdb.stream._
import OverflowStrategy.OverflowStrategy
import ActorSourceLiveQuery.{ ErrorOccurred, TokenFound, Enqueue }

import scala.util.Try

private[stream] class LiveQueryImpl(bufferSize: Int, overflowStrategy: OverflowStrategy)(query: String) extends LiveQuery {
  def execute(args: AnyRef*)(implicit db: ODatabaseDocumentTx, actorRefFactory: ActorRefFactory, loader: OrientLoader): Publisher[LiveQueryData] = {
    val actorRef = actorRefFactory.actorOf(Props(new ActorSourceLiveQuery(bufferSize, overflowStrategy)(db)))

    val listener = new OLiveResultListener {
      override def onLiveResult(iLiveToken: Int, iOp: ORecordOperation): Unit = {
        Try {
          // for some reason, thread which orientdb uses to call in listener doesn't
          // have db active ? Bug?
          if (!db.isActiveOnCurrentThread) db.activateOnCurrentThread
          val data = iOp.getRecord match {
            case document: ODocument ⇒
              loader(document)
              iOp.`type` match {
                case 0 ⇒ Loaded(document)
                case 1 ⇒ Updated(document)
                case 2 ⇒ Deleted(document)
                case 3 ⇒ Created(document)
              }
          }

          actorRef ! Enqueue(LiveQueryDataWithToken(data, iLiveToken))
        }.recover { case t ⇒ actorRef ! ErrorOccurred(t) }.get // throw exc even on Orient
      }
    }

    Try {
      // TODO: we don't really know what goes on, when the query fetching it truly throws exception,
      // and how to emit it... we'd probably need something like is Smart query wrapper...
      // - find a way how to make live query die, and make test that we get onError in stream...
      // -- maybe we can run this in future and hook onFailure or something
      val reply: OResultSet[ODocument] = db.query(new OLiveQuery[ODocument](query, listener), args: _*)
      val token: Integer = reply.get(0).field("token") // from orientdb documentation
      actorRef ! TokenFound(token)
    }.recover { case t ⇒ actorRef ! ErrorOccurred(t) }
    ActorPublisher[LiveQueryData](actorRef)
  }
}
