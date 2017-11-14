package com.example

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.model.{GlacierJobParameters, RestoreObjectRequest, Tier}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import scala.concurrent.duration._

object S3Client {
  def build(profileName: String): AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withCredentials(new ProfileCredentialsProvider(profileName))
      .build() 
}

object S3Retriever {
  def props(profileName: String): Props = Props(new S3Retriever(profileName))
  
  final case class RestoreObject(bucketName: String, key: String)
  final case class CheckRestoreJob(bucketName: String, key: String, initialStartTime: LocalDateTime)
}

class S3Retriever(profileName: String) extends Actor with ActorLogging {
  import S3Retriever._
  import context.dispatcher

  lazy val s3 = S3Client.build(profileName)

  def receive = {
    // Start a restore job
    case RestoreObject(bucketName, key) => {
      val gjp = new GlacierJobParameters().withTier(Tier.Expedited)
      val rop = new RestoreObjectRequest(bucketName, key)
        .withGlacierJobParameters(gjp)
        .withExpirationInDays(5)

      log.info(s"($profileName) About to restore s3 object from glacier with details: $rop")
      s3.restoreObject(rop)

      // Send a check status message to arrive in 1 second
      context.system.scheduler.scheduleOnce(1 second, self,
        CheckRestoreJob(bucketName, key, LocalDateTime.now))
    }

    // Check the status of a restore job
    case CheckRestoreJob(bucketName, key, initialStartTime) => {
      if (s3.getObjectMetadata(bucketName, key).getOngoingRestore) {
        log.info(s"($profileName) Object restore for file ($bucketName/$key) still in progress")
        context.system.scheduler.scheduleOnce(1 second, self,
          CheckRestoreJob(bucketName, key, initialStartTime))
      }
      else {
        val elapsedSeconds = ChronoUnit.SECONDS.between(initialStartTime, LocalDateTime.now)
        log.info(s"($profileName) Object restore for file ($bucketName/$key) completed in $elapsedSeconds ")
      }
    }
  }
}

object AwsQuickstart extends App {
  import S3Retriever._

  val system: ActorSystem = ActorSystem("awsQuickstart")

  // Per account actors
  val foxdev: ActorRef = system.actorOf(S3Retriever.props("foxdev"), "foxdevS3Actor")
  val foxdevhp: ActorRef = system.actorOf(S3Retriever.props("foxdevhp"), "foxdevhpS3Actor")

  // Bucket name
  val bucketName = "TBC"

  // Test Files
  val file1 = "CentOS-7-x86_64-Everything-1708.iso"
  val file2 = "wish i had others"
  val file3 = "wish i had others 2"

  // Start our restores
  //foxdev ! RestoreObject(bucketName, file1)
  foxdevhp ! RestoreObject(bucketName, file1)
}