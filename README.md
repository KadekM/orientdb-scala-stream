_Experimental library, let's see how it works._

## Orientdb Scala Stream

Library allows you to execute `non blocking queries` and `live queries` on database, and treat results as reactive stream.

Supported
- Nonblocking queries
- ~~Live queries~~
- Named query execution
- Parametrized query execution

## Non blocking queries
```scalar
val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person")
val src = Source(query.execute()).runForeach(println) // prints all the results
```
This will backpressure the databse - if there is no demand from downstream, database won't perform the fetch. Cancelling subscription will stop database from fetching next rows. 

```scala
val query = NonBlockingQueryBuffering[ODocument]("SELECT * FROM Person")
val src = Source(query.execute()).map(myMethod).filter(myFilter).runFold(...) 
```
This will start the query on database, and results will be aggregated as database provides them. They will be pushed downstream accordingly to reactive-streams specification (based on demand...). Cancelling subscription will not stop db from finishing query, but elements will no longer be buffered.

## Live queries
TODO


## Implicits

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

## FAQ
Why does `Source(query.execute()).runForeach(println)` produce inconstitent strings ? Sometimes with Person prefix, sometimes without ?
Depends on which thread gets to run the execution. It can actually be the thread that has database set in ThreadLocals, which then makes `ODocument.toString()` to fetch also schema. Please read _implicits loader part_.
