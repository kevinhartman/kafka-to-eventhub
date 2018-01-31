package mn.hart.kafka2eventhub

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

// TODO: Assert kafka params make sense
// TODO: Add back-pressure
// TODO: Optimize serialization and garbage collection. Should be no need for Serialization since no shuffles.
object Main extends App {
  val arguments = Arguments(args)

  val adapterFunction = arguments.adapterFunctionClass match {
    case Some(className) => AdapterHelper.findAdapterFunction(className) // TODO: log
    case None => DefaultAdapter.asInstanceOf[(ConsumerRecord[_, _]) => Array[Byte]]
  }

  // Get and validate custom Kafka params if present
  val userKafkaParams = arguments.kafkaParamsClass.map(AdapterHelper.findKafkaParams)
  val kafkaParams = KafkaParameters(arguments, userKafkaParams)

  if (!arguments.force) {
    KafkaParameters.validate(kafkaParams)
  }

  val conf = new SparkConf()
    .setIfMissing("spark.app.name", "kafka-to-eventhub")
    .setIfMissing("spark.master", "local[*]")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer") // TODO: ensure serialization correct

  val sparkContext = new SparkContext(conf)
  val ssc = new StreamingContext(sparkContext, Duration(arguments.batchDuration.toMillis)) // TODO: set batch window from config

  // Create direct kafka stream
  val stream = KafkaUtils.createDirectStream(ssc, LocationStrategies.PreferConsistent,
    ConsumerStrategies.Subscribe[AnyVal, AnyVal](arguments.topics, kafkaParams))

  stream.foreachRDD(rdd => {
    val kafkaOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

    val transformedRdd = rdd.map[Array[Byte]](adapterFunction)

    print(new String(List(transformedRdd.collect()(0)).head))

  })

  ssc.start()
  ssc.awaitTermination()
}
