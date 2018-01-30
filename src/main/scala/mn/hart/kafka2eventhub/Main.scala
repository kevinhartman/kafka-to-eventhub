package mn.hart.kafka2eventhub

import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object Main extends App {

  // Get checkpoint dir from args
  val adapterFunctionClass: Option[String] = Some("mn.hart.kafka2eventhub.DefaultAdapter")
  val kafkaParamsClass: Option[String] = None
  val topics: List[String] = List()

  val adapterFunction = adapterFunctionClass match {
    case Some(className) => AdapterHelper.findAdapterFunction(className)
    case None => DefaultAdapter
  }

  val kafkaParams = kafkaParamsClass match {
    case Some(className) => AdapterHelper.findKafkaParams(className)
    case None => DefaultKafkaParams()
  }

  val conf = new SparkConf()
    .setIfMissing("spark.app.name", "kafka-to-eventhub")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer") // TODO: ensure serialization correct

  val sparkContext = new SparkContext(conf)
  val ssc = new StreamingContext(sparkContext, Seconds(5)) // TODO: set batch window from config

  // Create direct kafka stream with brokers and topics
  val stream = KafkaUtils.createDirectStream(ssc, LocationStrategies.PreferConsistent,
    ConsumerStrategies.Subscribe[AnyVal, AnyVal](topics, kafkaParams))

}
