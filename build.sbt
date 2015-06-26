name := "Orientdb-scala-livequery"

organization := "wat"

version := "0.1"

scalaVersion := "2.11.7"

val orientDBVersion = "2.1-rc4"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-RC4",
  "com.orientechnologies" % "orientdb-core" % orientDBVersion withSources(),
  "com.orientechnologies" % "orientdb-graphdb" % orientDBVersion withSources(),
  "com.orientechnologies" % "orientdb-client" % orientDBVersion withSources(),
  "com.tinkerpop.blueprints" % "blueprints-core" % "2.6.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Compile withSources()
)
 
