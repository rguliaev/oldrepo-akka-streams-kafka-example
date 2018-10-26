package kafka

import actors.KafkaPersistentActor
import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.scalalogging.LazyLogging
import models.{AppConfig, IncomingChunk, Instrument}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import io.circe.syntax._
import IncomingChunk._
import io.circe.parser._
import scala.concurrent.{ExecutionContext, Future}

class KafkaConsumer(appConfig: AppConfig, topic: Instrument)
                   (implicit system: ActorSystem, ec: ExecutionContext, mat: Materializer) extends LazyLogging {

  val persistentActor = system.actorOf(Props(new KafkaPersistentActor(topic.toString)))

  val consumerSettings = ConsumerSettings(appConfig.consumerConfig, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(appConfig.kafkaConfig.toString)
    .withGroupId("group1")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000")

  def run: (Control, Future[Done]) = {
    logger.info(s"Starting $topic Consumer")

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topic.toString))
      .map { msg =>
        //logger.info(s"${msg.record.key} - ${msg.record.value}")
        parse(msg.record.value()).flatMap(_.as[IncomingChunk]) match {
          case Right(chunk) => persistentActor ! chunk
          case Left(ex) => logger.info(ex.getMessage)
        }

        msg.committableOffset
      }
      .mapAsync(5)(offset => offset.commitScaladsl())
      .toMat(Sink.ignore)(Keep.both)
      .run()
  }
}
