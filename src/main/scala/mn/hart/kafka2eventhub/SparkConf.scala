package mn.hart.kafka2eventhub

import org.apache.spark.SparkConf

object SparkConf {
  def apply(): SparkConf = new SparkConf()
    .setIfMissing("spark.app.name", "kafka-to-eventhub")
    .setIfMissing("spark.master", "local[*]")
}
