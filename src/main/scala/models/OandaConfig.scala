package models

import com.typesafe.config.Config

case class AppConfig(oandaConfig: OandaConfig, kafkaConfig: KafkaConfig, producerConfig: Config, consumerConfig: Config)
case class OandaConfig(endpoint: String, token: String, accountId: String)
case class KafkaConfig(host: String, port: Int) {
  override def toString: String = host + ":" + port
}
