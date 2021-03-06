name := "orientdb-scala-stream"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/KadekM/orientdb-scala-stream"))

organization := "com.marekkadek"

version := "0.5.5-SNAPSHOT"

scalaVersion := "2.11.7"

fork := true // because of OrientDb 

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.3",
  "com.orientechnologies" % "orientdb-server" % "2.1.10",
  "org.reactivestreams" % "reactive-streams-tck" % "1.0.0" % Test,
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % "2.0.3" % Test
)
 
scalacOptions ++= Seq(
"-Xlint",
 "-deprecation"
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
    <scm>
      <url>https://github.com/kadekm/orientdb-scala-stream</url>
      <connection>scm:git://github.com/kadekm/orientdb-scala-stream.git</connection>
    </scm>
    <developers>
      <developer>
        <id>kadekm</id>
        <name>Marek Kadek</name>
        <url>https://github.com/KadekM</url>
      </developer>
    </developers>
