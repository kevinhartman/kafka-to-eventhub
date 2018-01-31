package mn.hart.kafka2eventhub

import org.apache.kafka.clients.consumer.ConsumerRecord

object DefaultAdapter extends ((ConsumerRecord[Array[Byte], Array[Byte]]) => Array[Byte]) {
  override def apply(v1: ConsumerRecord[Array[Byte], Array[Byte]]): Array[Byte] = v1.value()
}