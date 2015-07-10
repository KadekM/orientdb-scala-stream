_Experimental library, let's see how it works._

## Orientdb Scala Stream

Library allows you to execute `non blocking queries` and `live queries` on database, and treat them as stream.

Supported
- Nonblocking queries
- ~~Live queries~~
- Named query execution
- Parametrized query execution

### Non blocking queries
```scalar
val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person")
val src = Source(query.execute())
```
This will backpressure the databse - if there is no demand from downstream, database won't perform the fetch. Cancelling subscription will stop database from fetching next rows. 

```scala
val query = NonBlockingQueryBuffering[ODocument]("SELECT * FROM Person")
val src = Source(query.execute())
```
This will start the query on database, and results will be aggregated as database provides them. They will be pushed downstream accordingly to reactive-streams specification (based on demand...). Cancelling subscription will not stop db from finishing query, but elements will no longer be buffered.

### Implicits

To execute the queries you need following implicits:
```scala
implicit val db: ODatabaseDocumentTx = ???
implicit val ec: ExecutionContext = ???
implicit val materializer = ActorMaterializer()
implicit val loader = orientdb.streams.OrientLoaderDeserializing()
```
##### Loader
Loader specified what to do with your entity before it is sent to the source of the stream (and thus possibly emmited downstreams). **It runs on thread which has database setup as ThreadLocal, so you can call methods which require it**. In stream operations, there is no such guarantee.

**This will most likely change in future.**

##### ExecutionContext
Async queries run on separate thread.

### Live queries
TODO
