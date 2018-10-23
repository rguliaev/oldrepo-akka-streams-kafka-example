package actors

import akka.actor.{Actor, ActorLogging}
import models.{HeartBeatChunk, PriceChunk}

class KafkaPublisherActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case priceChunk: PriceChunk => log.info(priceChunk.toString)
    case heartBeatChunk: HeartBeatChunk => log.info(heartBeatChunk.toString)
  }
}
