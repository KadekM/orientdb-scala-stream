name := "orientdb-scala-stream"

organization := "com.marekkadek"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-RC4",
  "com.typesafe.akka" % "akka-stream-testkit-experimental_2.11" % "1.0-RC4",
  "com.orientechnologies" % "orientdb-server" % "2.1-rc4",
  "org.reactivestreams" % "reactive-streams-tck" % "1.0.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Compile withSources()
)
 
scalacOptions ++= Seq(
"-Xlint",
// "-Xfatal-warnings",
// "-feature"
 "-deprecation"
//"-Xlog-implicits"
//"-Ydebug"
)
