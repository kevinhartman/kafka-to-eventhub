package mn.hart.kafka2eventhub

import org.apache.kafka.common.serialization.ByteArrayDeserializer

object DefaultKafkaParams extends (() => Map[String, Object]) {
  override def apply(): Map[String, Object] = Map(
    "key.deserializer" -> classOf[ByteArrayDeserializer],
    "value.deserializer" -> classOf[ByteArrayDeserializer]
  )
}
