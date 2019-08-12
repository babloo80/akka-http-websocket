import Dependencies._

ThisBuild / scalaVersion     := "2.12.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "akka-http-websocket",
    libraryDependencies ++= akkaTyped,
    libraryDependencies ++= akkaStreams,
    libraryDependencies += akkaHttp,
    libraryDependencies += json4s,
    libraryDependencies += json4sSerDe,
    libraryDependencies += redisClient,
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= akkaTest
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
