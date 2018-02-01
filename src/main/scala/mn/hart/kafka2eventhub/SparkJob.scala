package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.EventData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, OffsetRange}

object SparkJob {
  type KafkaRDD = RDD[ConsumerRecord[AnyVal, AnyVal]]
  type EventHubRDD = (RDD[EventData], Array[OffsetRange])

  def apply(arguments: Arguments, offsetCommitter: CanCommitOffsets): Function[KafkaRDD, Unit] = {

    // Load custom adapter function or use default if not specified
    val adapterFunction = arguments.adapterFunctionClass match {
      case Some(className) => AdapterHelper.findAdapterFunction(className) // TODO: log
      case None => DefaultAdapter.asInstanceOf[(ConsumerRecord[_, _]) => EventData]
    }

    val transformRDD: PartialFunction[KafkaRDD, EventHubRDD] = {
      case rdd =>
        val offsets = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        val transformedRdd = rdd.map[EventData](adapterFunction)

        (transformedRdd, offsets)
    }

    val uploadRDD: PartialFunction[EventHubRDD, EventHubRDD] = {
      case eventHubRdd =>
        val (rdd, offsetRange) = eventHubRdd

        eventHubRdd
    }

    val commitKafkaOffsets: PartialFunction[EventHubRDD, Unit] = {
      case (_, offsets) =>
        // All partitions have been written. Mark offsets as completed in Kafka.
        offsetCommitter.commitAsync(offsets)
    }

    transformRDD
      .andThen(
        arguments.compression match {
          case Some("gzip") =>
            { case (rdd, offsets) => (GZipCompress(rdd), offsets) }
          case _ =>
            // Skip compression
            { case (rdd, offsets) => (rdd, offsets)}
        }
      )
      .andThen(uploadRDD)
      .andThen(commitKafkaOffsets)
  }
}
