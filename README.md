## DO NOT USE THIS YET##
_Just experiment, let's see how it goes. Pretty much everything is work in progress._

## Orientdb Scala Stream

Library allows you to execute `non blocking queries` and `live queries` on database, and treat them as stream.

### Non blocking quries
```scala
val query = NonBlockingQueryBuffering[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 3")
val src = Source(query.execute())
```
This will start the query on database, and results will be aggregated as database provides them. They will be pushed downstream accordingly to reactive-streams specification (based on demand...).

```scala
val query = NonBlockingQueryBackpressuring[ODocument]("SELECT * FROM Person ORDER BY name LIMIT 3")
val src = Source(query.execute())
```
This will backpressure the databse - if there is no demand from downstream, database won't perform the fetch.


### Live queries
TODO
