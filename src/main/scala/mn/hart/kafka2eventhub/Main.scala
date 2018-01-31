package mn.hart.kafka2eventhub

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

// TODO: Assert kafka params make sense
object Main extends App {
  val arguments = Arguments(args)

  val adapterFunction = arguments.adapterFunctionClass match {
    case Some(className) => AdapterHelper.findAdapterFunction(className) // TODO: log
    case None => DefaultAdapter.asInstanceOf[(ConsumerRecord[_, _]) => Array[Byte]]
  }

  val kafkaParams = arguments.kafkaParamsClass match {
    case Some(className) => AdapterHelper.findKafkaParams(className) // TODO log
    case None => DefaultKafkaParams()
  }

  val conf = new SparkConf()
    .setIfMissing("spark.app.name", "kafka-to-eventhub")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer") // TODO: ensure serialization correct

  val sparkContext = new SparkContext(conf)
  val ssc = new StreamingContext(sparkContext, Seconds(5)) // TODO: set batch window from config

  // Create direct kafka stream with brokers and topics
  val stream = KafkaUtils.createDirectStream(ssc, LocationStrategies.PreferConsistent,
    ConsumerStrategies.Subscribe[AnyVal, AnyVal](arguments.topics, kafkaParams))

  stream.foreachRDD(rdd => {
    val kafkaOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

    val transformedRdd = rdd.map[Array[Byte]](adapterFunction)

    transformedRdd
  })
}
