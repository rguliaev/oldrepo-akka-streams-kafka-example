package models

trait Instrument
case object EURUSD extends Instrument {
  override def toString: String = "EUR_USD"
}
case object GBPUSD extends Instrument {
  override def toString: String = "GBP_USD"
}
case object AUDUSD extends Instrument {
  override def toString: String = "AUD_USD"
}
