package mn.hart.kafka2eventhub

import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.SparkContext

// TODO: Assert kafka params make sense
// TODO: Add back-pressure
// TODO: Optimize garbage collection.
object Main extends App {
  val arguments = Arguments(args)

  // Get and validate custom Kafka params if present
  val userKafkaParams = arguments.kafkaParamsClass.map(AdapterHelper.findKafkaParams)
  val kafkaParams = KafkaParameters(arguments, userKafkaParams)

  if (!arguments.force) {
    KafkaParameters.validate(kafkaParams)
  }

  val sparkContext = new SparkContext(SparkConf())
  val ssc = new StreamingContext(sparkContext, Duration(arguments.batchDuration.toMillis)) // TODO: set batch window from config

  // Create direct kafka stream
  val stream = KafkaUtils.createDirectStream(ssc, LocationStrategies.PreferConsistent,
    ConsumerStrategies.Subscribe[AnyVal, AnyVal](arguments.topics, kafkaParams))

  stream.foreachRDD(SparkJob(arguments, stream.asInstanceOf[CanCommitOffsets]))

  ssc.start()
  ssc.awaitTermination()
}