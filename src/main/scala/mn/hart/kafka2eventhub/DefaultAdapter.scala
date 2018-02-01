package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.EventData
import org.apache.kafka.clients.consumer.ConsumerRecord

object DefaultAdapter extends ((ConsumerRecord[Array[Byte], Array[Byte]]) => EventData) with Serializable {
  override def apply(v1: ConsumerRecord[Array[Byte], Array[Byte]]): EventData = new EventData(v1.value())
}