_Experimental library, let's see how it works._

_If you are missing functionality, or something doesn't work, please either raise issue or make a PR. See info at bottom._

## Orientdb Scala Stream

Library allows you to execute `non blocking queries` and `live queries` on database, and treat results as reactive stream.

Supported
- Nonblocking queries
- Live queries (experimental - see OrientDb)
- Named query execution
- Parametrized query execution

## Live queries
[(OrientDB documentation)](http://orientdb.com/docs/last/Live-Query.html)

Example:
```scala
import orientdb.streams._

implicit val db: ODatabaseDocumentTx = ???
implicit val loader = OrientLoaderDeserializing()

val query = LiveQuery("LIVE SELECT FROM Person")
Source(query.execute())
    .collect{ case Created(data) => data }
    .takeWhile(_.field("name").toString().contains("Pet"))
    .runForeach(println)
```
Once you execute live query you get notifications for following events as they are streamed from OrientDB:
- Loaded
- Updated
- Deleted
- Created

The listener is automatically unsubscribed from OrientDB once subscription is cancelled (a command `live unsubscribe TOKEN_VALUE`)

## Non blocking queries
[(OrientDB documentation)](http://orientdb.com/docs/last/Document-Database.html#non-blocking-query-since-v21)
```scala
val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person")
val src = Source(query.execute())
          .runForeach(println) // prints all the results
```
This will backpressure the databse - if there is no demand from downstream, database won't perform the fetch. Cancelling subscription will stop database from fetching next rows. 

```scala
val query = NonBlockingQueryBuffering[ODocument]("SELECT * FROM Person WHERE name = :lookingFor")
val src = Source(query.executeNamed("lookingFor" -> "Peter"))
          .map(myMethod)
          .filter(myFilter)
          .runFold(...) 
```
This will start the query on database, and results will be aggregated as database provides them. They will be pushed downstream accordingly to reactive-streams specification (based on demand...). Cancelling subscription will not stop db from finishing query, but elements will no longer be buffered.

## Loader

To execute the queries you need following implicit (beside the regular ones):
```scala
implicit val loader = OrientLoaderDeserializing()
```
Loader specified what to do with your entity before it is sent to the source of the stream (and thus possibly emmited downstreams). **It runs on thread which has database setup as ThreadLocal, so you can call methods which require it**. In stream operations, there is no such guarantee.
You can implement your `OrientLoader`.

**The way this works may change in future.**

## FAQ
Why does `Source(query.execute()).runForeach(println)` produce inconstitent strings ? Sometimes with Person prefix, sometimes without ?
* Depends on which thread gets to run the execution. It can actually be the thread that has database set in ThreadLocals, which then makes `ODocument.toString()` to fetch also schema. Please read _implicits loader part_.

## Info
This library is yet in very early stage. There are lots of TODOs, and often you may discover better way to implement something - feel free to raise issue or submit PR, I will very much welcome it.
