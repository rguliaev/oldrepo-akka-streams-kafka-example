package stream

import java.nio.charset.StandardCharsets
import akka.Done
import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Query
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{Framing, Keep, Sink, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import models.{AppConfig, IncomingChunk, Instrument}
import io.circe.parser._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import io.circe.syntax._

class OandaStreamConnector(appConfig: AppConfig, instrument: Instrument)
                          (implicit system: ActorSystem, ec: ExecutionContext, mat: Materializer) extends LazyLogging {

  val url = s"${appConfig.oandaConfig.endpoint}/v3/accounts/${appConfig.oandaConfig.accountId}/pricing/stream"

  val producerSettings = ProducerSettings(appConfig.producerConfig, new StringSerializer, new StringSerializer)
    .withBootstrapServers(appConfig.kafkaConfig.toString)


  def run: (UniqueKillSwitch, Future[Done]) = {
    logger.info(s"Starting $instrument Stream")
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = Uri(url).withQuery(Query("instruments" -> instrument.toString))
    ).addCredentials(HttpCredentials.createOAuth2BearerToken(appConfig.oandaConfig.token))

    val httpRequest = Http().singleRequest(request)

    Source.fromFuture(httpRequest).viaMat(KillSwitches.single)(Keep.right).flatMapConcat { response =>
      response.entity.withoutSizeLimit.getDataBytes
        .via(Framing delimiter(ByteString("\n"), Int.MaxValue))
    }
    .groupedWithin(5, 5 seconds)
    .map { bytesSeq =>
      val messagesList = bytesSeq.map { bytes =>
        val jsonString = bytes.decodeString(StandardCharsets.UTF_8)
        parse(jsonString).flatMap(_.as[IncomingChunk])
      }.foldLeft(List.empty[ProducerRecord[String, String]]) { (result, parsedEither) =>
        parsedEither match {
          case Right(chunk) =>
            result :+ new ProducerRecord(instrument.toString, chunk.id, chunk.asJson.noSpaces)
          case Left(ex) =>
            logger.info(ex.getMessage)
            result
        }
      }

      new ProducerMessage.MultiMessage[String, String, Int](
        messagesList,
        messagesList.size
      )
    }
    .via(Producer.flexiFlow(producerSettings))
    .toMat(Sink.ignore)(Keep.both).run
  }
}
