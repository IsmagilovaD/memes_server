ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "memes_server",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.2.6",
      "com.typesafe.akka" %% "akka-stream" % "2.6.16",
      "org.typelevel" %% "cats-effect" % "3.5.0",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC2", // HikariCP transactor.
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC2", // Postgres driver 42.3.1 + type mappings.
      "io.estatico" %% "newtype" % "0.4.4",
      "com.github.pureconfig" %% "pureconfig" % "0.17.4",
      "commons-codec" % "commons-codec" % "1.15",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6",
      "ch.qos.logback" % "logback-classic" % "1.2.6",
      "org.scalatest" %% "scalatest" % "3.2.8",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.16",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.2.6",
      "org.mockito" %% "mockito-scala" % "1.16.46" % Test
    )
  )
