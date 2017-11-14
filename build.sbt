// Dependency versions
lazy val akkaVersion = "2.5.3"
lazy val awsVersion = "1.11.228"

// Dependencies
lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
lazy val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
lazy val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % awsVersion

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"


// Project definition
lazy val akkaQuickstart = project
  .in(file("."))
  .settings(
    inThisBuild(List(
      name := "akkaQuickstart",
      organization := "com.example",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT",
      scalacOptions ++= Seq("-unchecked", "-deprecation"),
    )),
    libraryDependencies ++= Seq(
      akkaActor,
      awsS3,
      akkaTestKit % Test,
      scalaTest % Test
    )
  )