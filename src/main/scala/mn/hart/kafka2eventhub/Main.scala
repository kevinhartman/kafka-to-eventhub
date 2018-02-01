package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.EventData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

// TODO: Assert kafka params make sense
// TODO: Add back-pressure
// TODO: Optimize serialization and garbage collection. Should be no need for Serialization since no shuffles.
object Main extends App {
  val arguments = Arguments(args)

  val adapterFunction = arguments.adapterFunctionClass match {
    case Some(className) => AdapterHelper.findAdapterFunction(className) // TODO: log
    case None => DefaultAdapter.asInstanceOf[(ConsumerRecord[_, _]) => EventData]
  }

  // Get and validate custom Kafka params if present
  val userKafkaParams = arguments.kafkaParamsClass.map(AdapterHelper.findKafkaParams)
  val kafkaParams = KafkaParameters(arguments, userKafkaParams)

  if (!arguments.force) {
    KafkaParameters.validate(kafkaParams)
  }

  // TODO: move to separate function
  val conf = new SparkConf()
    .setIfMissing("spark.app.name", "kafka-to-eventhub")
    .setIfMissing("spark.master", "local[*]")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

  val sparkContext = new SparkContext(conf)
  val ssc = new StreamingContext(sparkContext, Duration(arguments.batchDuration.toMillis)) // TODO: set batch window from config

  // Create direct kafka stream
  val stream = KafkaUtils.createDirectStream(ssc, LocationStrategies.PreferConsistent,
    ConsumerStrategies.Subscribe[AnyVal, AnyVal](arguments.topics, kafkaParams))

  stream.foreachRDD(rdd => {
    val kafkaOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

    val transformedRdd = rdd.map[EventData](adapterFunction)

    // TODO: do this at stream level to skip check each RDD
    val compressedRdd = arguments.compression match {
      case Some("gzip") =>
        GZipCompress(transformedRdd)
      case _ =>
        // Skip compression
        transformedRdd
    }

    // TODO: Write all partitions into EventHub as batches here
    //

    // All partitions have been written. Mark offsets as completed in Kafka.
    stream.asInstanceOf[CanCommitOffsets].commitAsync(kafkaOffsetRanges)
  })

  ssc.start()
  ssc.awaitTermination()
}
