name := "orientdb-scala-stream"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/KadekM/orientdb-scala-stream"))

organization := "com.marekkadek"

version := "0.2"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-RC4",
  "com.orientechnologies" % "orientdb-server" % "2.1-rc5",
  "org.reactivestreams" % "reactive-streams-tck" % "1.0.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "com.typesafe.akka" % "akka-stream-testkit-experimental_2.11" % "1.0-RC4" % Test
)
 
scalacOptions ++= Seq(
"-Xlint",
 "-deprecation"
)
