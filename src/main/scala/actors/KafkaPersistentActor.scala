package actors

import akka.actor.{Actor, Props}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import com.typesafe.scalalogging.LazyLogging
import models.IncomingChunk


case class Msg(deliveryId: Long, chunk: IncomingChunk)
case class Confirm(deliveryId: Long)

sealed trait Evt
case class MsgSent(chunk: IncomingChunk) extends Evt
case class MsgConfirmed(deliveryId: Long) extends Evt

case class EventStore(events: List[IncomingChunk]) {
  def update(event: IncomingChunk) = copy(events :+ event)
}

class KafkaPersistentActor(val persistenceId: String) extends Actor
  with PersistentActor with AtLeastOnceDelivery with LazyLogging {

  val stateActor = context.system.actorOf(Props(new KafkaPersistenceStateActor(persistenceId)))

  override def receiveRecover: Receive = {
    case evt: Evt => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case chunk: IncomingChunk => persist(MsgSent(chunk))(updateState)
    case Confirm(deliveryId) ⇒ persist(MsgConfirmed(deliveryId))(updateState)
  }

  def updateState(evt: Evt): Unit = evt match {
    case MsgSent(chunk) => deliver(stateActor.path)(deliveryId ⇒ Msg(deliveryId, chunk))
    case MsgConfirmed(deliveryId) => confirmDelivery(deliveryId)
  }
}

class KafkaPersistenceStateActor(val persistenceId: String) extends Actor with PersistentActor with LazyLogging {
  private var store: EventStore = EventStore(Nil)

  override def receiveRecover: Receive = {
    case chunk: IncomingChunk => store = store.update(chunk)
  }

  override def receiveCommand: Receive = {
    case Msg(deliveryId, chunk) ⇒ persist(chunk) { event =>
      store = store.update(event)
      logger.info(s"${store.events.length}")
      sender() ! Confirm(deliveryId)
    }
  }
}





