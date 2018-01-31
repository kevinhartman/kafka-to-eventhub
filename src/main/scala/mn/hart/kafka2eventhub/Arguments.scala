package mn.hart.kafka2eventhub

import scala.concurrent.duration.Duration

case class Arguments(
  batchDuration: Duration = null,
  brokerList: Seq[String] = List(),
  zookeeper: String = null,
  groupId: String = null,
  topics: Seq[String] = List(),
  adapterFunctionClass: Option[String] = None,
  kafkaParamsClass: Option[String] = None
)

object Arguments {
  def apply(arguments: Seq[String]): Arguments = {
    val parser = new scopt.OptionParser[Arguments]("kafka-to-eventhub") {
      head("kafka-to-eventhub", "1.x")

      opt[Duration]("duration").required().valueName("<duration>").action((d, args) => args.copy(batchDuration = d))
        .text("Spark Streaming batch duration")

      opt[Seq[String]]("broker-list").required().valueName("<broker1>,[<broker2>...]").action((b, args) => args.copy(brokerList = b))
        .text("Kafka broker list")
        .validate(b => if (b.nonEmpty) success else failure("Value <broker1> must be present"))

      opt[String]("zookeeper").required().valueName("<zookeeper>").action((z, args) => args.copy(zookeeper = z))
        .text("Zookeeper hostname")

      opt[String]("group-id").required().valueName("<groupid>").action((g, args) => args.copy(groupId = g))
        .text("Kafka group ID used for commit log")

      opt[Seq[String]]("topics").required().valueName("<topic1>,[<topic2>...]").action((t, args) => args.copy(topics = t))
        .text("Kafka topics to forward into EventHub")
        .validate(t => if (t.nonEmpty) success else failure("Value <topic1> must be present"))

      opt[String]("adapter-object").optional().valueName("<namespace>.<objectname>").action((o, args) => args.copy(adapterFunctionClass = Some(o)))
        .text("Fully qualified Scala root-level object name of function to use when converting deserialized data from Kafka to EventHub format")

      opt[String]("kafka-params-object").optional().valueName("<namespace>.<objectname>").action((o, args) => args.copy(kafkaParamsClass = Some(o)))
        .text("Fully qualified Scala root-level object name of function supplying a custom Kafak parameter map")

      note(
        """Customization
          |=============
          |
          |Use the arguments "adapter-object" and "kafka-params-object" to specify a custom function to use when converting
          |from deserialized Kafka events to the EventHub event format (Byte[]) and a custom Kafka param map, respectively.
          |
          |adapter-object
          |--------------
          |Fully qualified Scala root-level object name of function to use when converting deserialized data from Kafka to
          |EventHub format.
          |
          |The following example object's name should be passed as:  "com.contoso.MyAdapter"
          |
          |Ex:
          |  package com.contoso
          |
          |  object MyAdapter extends ((ConsumerRecord[MyKeyType, Array[MyValueType]]) => Array[Byte]) {
          |    def apply(v1: ConsumerRecord[MyKeyType, MyValueType]): Array[Byte] = ???
          |  }
          |
          |Note:
          |  Specified class must be on the class path (add extra args bundle with spark-submit).
          |
          |  MyKeyType and MyValueType specified above must also be on the class path, and must be deserializable using
          |  the deserializers specified in the KafkaParams map ("key.deserializer" and "value.deserializer", respectively).
          |
          |
          |kafka-params-object
          |-------------------
          |Fully qualified Scala root-level object name of function supplying a custom Kafak parameter map.
          |
          |The following example object's name should be passed as:  "com.contoso.MyParams"
          |
          |Ex:
          |  package com.contoso
          |
          |  object MyParams extends (() => Map[String, Object]) {
          |    def apply(): Map[String, Object] = Map(
          |      "key.deserializer" -> classOf[MyValueTypeDeserializer],
          |      "value.deserializer" -> classOf[MyKeyTypeDeserializer]
          |    )
          |  }
          |
          |Note:
          |  Specified class must be on the class path (add extra args bundle with spark-submit).
        """.stripMargin
      )
    }

    parser.parse(arguments, Arguments()) match {
      case Some(config) => config
      case None => sys.exit(1)
    }
  }
}