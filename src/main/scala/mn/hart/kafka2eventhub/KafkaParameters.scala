package mn.hart.kafka2eventhub

object KafkaParameters {
  def apply(arguments: Arguments, userParams: Option[Map[String, Object]]): Map[String, Object] = {
    val kafkaFromArgs = Map[String, Object]() ++
      arguments.zookeeper.map("zookeeper" -> _) ++
      arguments.groupId.map("group.id" -> _) ++
      arguments.brokerList.map("bootstrap.servers" -> _)

    // Overlay user params over the defaults, and overlay commandline args over that
    DefaultKafkaParams() ++ userParams.getOrElse(Map()) ++ kafkaFromArgs
  }

  def validate(params: Map[String, Object]) : Unit = {
    def missingErr(paramName: String): String = {
      s"Kafka parameter '$paramName' must be specified either through commandline arg or custom Kafka param map object."
    }

    params.getOrElse("zookeeper", throw new Exception(missingErr("zookeeper")))
    params.getOrElse("group.id", throw new Exception(missingErr("group.id")))
    params.getOrElse("bootstrap.servers", throw new Exception(missingErr("bootstrap.servers")))

    if (params.getOrElse("enable.auto.commit", true) == true) {
      throw new Exception("Auto commit must be disabled.")
    }
  }
}
