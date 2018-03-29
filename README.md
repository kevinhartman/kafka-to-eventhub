# Overview
This purpose of this project is to mirror data from a Kafka instance into Azure EventHub in real time. It consists of an Apache Spark streaming application which allows it to scale out to match the parallelism (# of partitions) of the source Kafka topic(s).

To use this tool, build it and deploy it to a Spark cluster. It should work out of the box with minimal configuration. See the deployment section below for a description of available parameters.

# Events
By default, Kafka messages (records) are transformed into EventHub events simply by dropping the message's key. In other words, the value of the Kafka message is forwarded to EventHub verbatim, while the key is ignored. Recall that Kafka messages are key-value pairs, while EventHub events are simply values.

If you need to transform Kafka messages differently, use a custom event adapter as described below. As an example, perhaps you'd like to combine the Kafka message's key and value into a JSON object, sending that along to EventHub.

## Custom Event Adapters
To define a custom transformation funciton to convert Kafka messages into EventHub events, define a custom event adapter. This is done by writing Scala code (conforming to an interface), and including the compiled module on the classpath when submitting the `kafka-to-eventhub` tool to the Spark cluster.

### Code Example
To implement a custom adapter, use the following as a template to define a root-level Scala object. See the [DefaultAdapter](src/main/scala/mn/hart/kafka2eventhub/DefaultAdapter.scala) for an example.

```scala
package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.EventData
import org.apache.kafka.clients.consumer.ConsumerRecord

object MyCustomAdapter extends ((ConsumerRecord[Array[Byte], Array[Byte]]) => EventData) with Serializable {
  override def apply(v1: ConsumerRecord[Array[Byte], Array[Byte]]): EventData = {
    // Custom conversion code here (e.g. v1.value())
  }
}
```

# Notes
- At-least-once semantics are guaranteed, meaning each event in Kafka will be placed into the EventHub topic at least once, but not exactly once (duplicates are possible). Consequently, downstream EventHub topic consumers must be able to handle duplicates.
