package models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import TimeFormatter._

object TimeFormatter {
  val datetimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
}

trait IncomingChunk {
  val `type`: String
  val time: LocalDateTime
  def id = `type`+ "-" + time.format(datetimeFormat)
}

object IncomingChunk {
  implicit val encoder: Encoder[IncomingChunk] = Encoder.instance {
    case priceChunk @ PriceChunk(_, _, _, _, _, _, _, _, _) => priceChunk.asJson
    case heartBeat @ HeartBeatChunk(_, _) => heartBeat.asJson
  }

  implicit val decoder: Decoder[IncomingChunk] = new Decoder[IncomingChunk] {
    final def apply(c: HCursor): Decoder.Result[IncomingChunk] = {
      c.downField("type").as[String].toOption match {
        case Some("PRICE") =>
          for {
            time <- c.downField("time").as[LocalDateTime](Decoder.decodeLocalDateTimeWithFormatter(datetimeFormat))
            bids <- c.downField("bids").as[List[PriceData]]
            asks <- c.downField("asks").as[List[PriceData]]
            closeoutBid <- c.downField("closeoutBid").as[BigDecimal]
            closeoutAsk <- c.downField("closeoutAsk").as[BigDecimal]
            status <- c.downField("status").as[String]
            tradeable <- c.downField("tradeable").as[Boolean]
            instrument <- c.downField("instrument").as[String]
          } yield {
            PriceChunk("PRICE", time, bids, asks, closeoutBid, closeoutAsk, status, tradeable, instrument)
          }
        case Some("HEARTBEAT") =>
          for {
            time <- c.downField("time").as[LocalDateTime](Decoder.decodeLocalDateTimeWithFormatter(datetimeFormat))
          } yield {
            HeartBeatChunk("HEARTBEAT", time)
          }
        case _ => Left(DecodingFailure("Unknown message", Nil))
      }

    }
  }
}
case class PriceData(price: BigDecimal, liquidity: Long)
case class PriceChunk(
  `type`: String,
  time: LocalDateTime,
  bids: List[PriceData],
  asks: List[PriceData],
  closeoutBid: BigDecimal,
  closeoutAsk: BigDecimal,
  status: String,
  tradeable: Boolean,
  instrument: String) extends IncomingChunk

case class HeartBeatChunk(`type`: String, time: LocalDateTime) extends IncomingChunk
