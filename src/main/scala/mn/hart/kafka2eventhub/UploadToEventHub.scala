package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.EventData
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._
import scala.language.implicitConversions

object UploadToEventHub {
  private implicit def iteratorToIterable[A](iterator: java.util.Iterator[A]): java.lang.Iterable[A] = {
    iterator.asScala.toIterable.asJava
  }

  def apply(arguments: Arguments, rdd: RDD[EventData]): Unit = {
    val connStr = s"${arguments.eventHubConnStr};EntityPath=${arguments.eventHubName}"

    rdd.foreachPartition(partition => {
      if (partition.hasNext) {
        val client = EventHubClient(connStr)

        var batch = client.createBatch()
        while (partition.hasNext) {
          val event = partition.next

          if (!batch.tryAdd(event)) {
            if (batch.getSize > 0) {
              // Send filled batch to EventHub
              client.sendSync(batch.iterator)

              // Initialize a new empty batch
              batch = client.createBatch()
            }

            // Retry adding event into empty batch
            if (!batch.tryAdd(event)) {
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