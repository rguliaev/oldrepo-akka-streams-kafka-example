name := "oanda-streaming"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= {
  val akkaVersion = "2.5.17"
  val kafkaVersion = "2.0.0"
  val akkaHttpVersion = "10.1.5"
  val circleVersion = "0.9.3"

  Seq(
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.22",
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.22.0",
      "org.apache.kafka" %% "kafka" % kafkaVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  ) ++
  Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser").map(_ % circleVersion)
}
