import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest" %% "scalatest" % "3.0.8"
  val akkaVersion     = "2.5.23"
  val akkaHttpVersion = "10.1.9"
  val json4sVersion   = "3.6.7"
  lazy val akkaTyped  = Seq("com.typesafe.akka" %% "akka-actor-typed" % akkaVersion)
  lazy val akkaTest = Seq(
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
    "com.typesafe.akka" %% "akka-stream-testkit"      % akkaVersion     % Test,
    "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test
  )
  lazy val akkaHttp    = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  lazy val akkaStreams = Seq("com.typesafe.akka" %% "akka-stream" % akkaVersion, "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion)
  lazy val json4s      = "org.json4s" %% "json4s-jackson" % json4sVersion
  lazy val json4sSerDe = "de.heikoseeberger" %% "akka-http-json4s" % "1.27.0" //disable transitive dep?
  lazy val redisClient = "net.debasishg" %% "redisclient" % "3.10"
}
