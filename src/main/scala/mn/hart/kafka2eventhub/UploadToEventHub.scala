package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.{EventData, EventDataBatch}
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object UploadToEventHub {
  private implicit def iteratorToIterable[A](iterator: java.util.Iterator[A]): java.lang.Iterable[A] = {
    iterator.asScala.toIterable.asJava
  }

  /**
    * Workaround for https://github.com/Azure/azure-event-hubs-java/issues/235
    *
    * TODO: delete this extension once fix is released.
    * @param value original [[EventDataBatch]] object on which to bind extension method.
    */
  implicit class EventDataBatchWorkaround(val value: EventDataBatch) extends AnyVal {
    def tryAddWorkaround(event: EventData): Boolean = Try(value.tryAdd(event)) match {
      case Success(result) => result
      case Failure(_: java.nio.BufferOverflowException) => false
      case Failure(unexpectedEx) => throw unexpectedEx
    }
  }

  def apply(arguments: Arguments, rdd: RDD[EventData]): Unit = {
    val connStr = s"${arguments.eventHubConnStr};EntityPath=${arguments.eventHubName}"

    rdd.foreachPartition(partition => {
      if (partition.hasNext) {
        val client = EventHubClient(connStr)

        var batch = client.createBatch()
        while (partition.hasNext) {
          val event = partition.next

          if (!batch.tryAddWorkaround(event)) {
            if (batch.getSize > 0) {
              // Send filled batch to EventHub
              client.sendSync(batch.iterator)

              // Initialize a new empty batch
              batch = client.createBatch()
            }

            // Retry adding event into empty batch
            if (!batch.tryAddWorkaround(event)) {
              // Single event > 256KB. For now, send alone to trigger correct message too big error.
              client.sendSync(event)
            }
          }
        }

        if (batch.getSize > 0) {
          // Send batch to EventHub
          client.sendSync(batch.iterator)
        }
      }
    })
  }
}