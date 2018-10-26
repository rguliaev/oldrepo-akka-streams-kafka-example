This is a demo project with Akka-Streams, Akka-Persistance and Kafka

You should have installed Kafka on machine

The logic is simple, App connects with a stream from Oanda and gets Json, further it stores to Kafka. 
The Consumer Actor consumes it from Kafka and sends to Persistence Actor that stores events in journal and sends to 
KafkaPersistenceStateActor with AtLeastOnceDelivery approach. KafkaPersistenceStateActor is persistent as well, it just 
stores IncomingChunk-s to journal.

As the ouput you will get something like 

14:07:50.219 [default-akka.actor.default-dispatcher-35] INFO  actors.KafkaPersistenceStateActor - 291
14:07:50.220 [default-akka.actor.default-dispatcher-7] INFO  actors.KafkaPersistenceStateActor - 292
14:07:50.221 [default-akka.actor.default-dispatcher-7] INFO  actors.KafkaPersistenceStateActor - 293

It shows length of List[IncomingChunk] stored in KafkaPersistenceStateActor

That's all



