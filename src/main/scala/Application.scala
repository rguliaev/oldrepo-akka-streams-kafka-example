import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import kafka.KafkaConsumer
import models._
import stream.OandaStreamConnector
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object Application extends App with OandaConfigLoader with LazyLogging {
  override def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    load match {
      case Success(appConfig) =>
        val (killSwitchEURUSD, streamEURUSD) = new OandaStreamConnector(appConfig, EURUSD).run
        val (killSwitchGBPUSD, streamGBPUSD)  = new OandaStreamConnector(appConfig, GBPUSD).run
        val (killSwitchAUDUSD, streamAUDUSD)  = new OandaStreamConnector(appConfig, AUDUSD).run

        val (consumerControlEURUSD, consumerStreamEURUSD) = new KafkaConsumer(appConfig, EURUSD).run
        val (consumerControlGBPUSD, consumerStreamGBPUSD) = new KafkaConsumer(appConfig, GBPUSD).run
        val (consumerControlAUDUSD, consumerStreamAUDUSD) = new KafkaConsumer(appConfig, AUDUSD).run

        sys.addShutdownHook {
          logger.info("Shutting down...")
          consumerControlEURUSD.shutdown()
          consumerControlGBPUSD.shutdown()
          consumerControlAUDUSD.shutdown()

          killSwitchEURUSD.shutdown()
          killSwitchGBPUSD.shutdown()
          killSwitchAUDUSD.shutdown()

          val theEnd = for {
            _ <- consumerStreamEURUSD
            _ <- streamEURUSD
            _ <- consumerStreamGBPUSD
            _ <- streamGBPUSD
            _ <- consumerStreamAUDUSD
            _ <- streamAUDUSD
            _ <- Http().shutdownAllConnectionPools()
            _ <- system.terminate()
          } yield ()

          theEnd.onComplete(_ => logger.info("Application has been stopped"))
        }
      case Failure(ex) => logger.info(ex.getMessage)
    }
  }
}

trait OandaConfigLoader {
  def load: Try[AppConfig] = Try {
    val config = ConfigFactory.load()
    val oandaEndpoint = config.getString("oanda.url")
    val oandaToken = config.getString("oanda.token")
    val oandaAccountId = config.getString("oanda.accountId")

    val oandaConfig = OandaConfig(oandaEndpoint, oandaToken, oandaAccountId)

    val kafkaHost = config.getString("akka.kafka.host")
    val kafkaPort = config.getInt("akka.kafka.port")

    val kafkaConfig = KafkaConfig(kafkaHost, kafkaPort)

    val producerConfig = config.getConfig("akka.kafka.producer")
    val consumerConfig = config.getConfig("akka.kafka.consumer")

    AppConfig(oandaConfig, kafkaConfig, producerConfig, consumerConfig)
  }
}
