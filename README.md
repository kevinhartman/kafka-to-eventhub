# Kafka to EventHub Mirror
This purpose of this project is to mirror data from a Kafka instance into Azure EventHub in real time. It consists of an Apache Spark streaming application which allows it to scale out to match the parallelism (# of partitions) of the source Kafka topic(s).

# Deployment
To use this tool, build it and deploy to a Spark cluster. It should work out of the box with minimal configuration. The following section lists the available parameters and their functions (passed as program arguments).

## Parameters
| Parameter | Description |
| --- | --- |
`--duration` | Spark Streaming batch duration
`--eh-conn` | EventHub connection string
`--eh-name` | EventHub name
`--topics` |  Comma-separated list of Kafka topics to forward into the EventHub
`--broker-list` | Comma-separated list of Kafka brokers
`--zookeeper` | Zookeeper hostname
`--group-id` | Kafka group ID used for commit log
`--adapter-object` | Fully qualified Scala root-level object name of function to use when converting deserialized data from Kafka to EventHub format
`--kafka-params-object` | Fully qualified Scala root-level object name of function supplying a custom Kafak parameter map
`--compression` | Compress events sent to EventHub using the specified format (currently supports 'gzip' only). If compression is used, EventHub consumers must be capable of decompressing data. For example, Azure Stream Analytics supports automatic decompression from an EventHub, which can be configured when adding a new input source.
`--force` | Skip validation of Kafka parameters

## Submission
The example command below demonstrates the minimal configuration, and assumes the mirroring tool is available in HDFS as `hdfs://kafka-to-eventhub.jar`. Submit this job to your production cluster just as you would any other workload, subbing in the correct parameter values for your environment.

```sh
spark-submit hdfs://kafka-to-eventhub.jar \
  --duration 5s \
  --broker-list localhost:9092 \
  --zookeeper localhost:2181 \
  --group-id test1 \
  --topics test \
  --eh-conn "Endpoint=..." \
  --eh-name kafka-to-eventhub
```

# Events
By default, Kafka messages (records) are transformed into EventHub events simply by dropping the message's key. In other words, the value of the Kafka message is forwarded to EventHub verbatim, while the key is ignored. Recall that Kafka messages are key-value pairs, while EventHub events are single values.

If you need to transform Kafka messages differently, use a custom event adapter as described below. As an example, perhaps you'd like to combine the Kafka message's key and value into a JSON object, sending that along to EventHub.

## Custom event adapters
To define a custom transformation function to convert Kafka messages into EventHub events, define a custom event adapter. This is done by writing Scala code and including its packaged JAR when deploying the mirroring tool to the Spark cluster.

### Code example
To implement a custom adapter, use the following as a template to define a root-level Scala object. See the [DefaultAdapter](src/main/scala/mn/hart/kafka2eventhub/DefaultAdapter.scala) for a working example. Note that the package name is unimportant.

```scala
package com.example.adapters

import com.microsoft.azure.eventhubs.EventData
import org.apache.kafka.clients.consumer.ConsumerRecord

object MyCustomAdapter extends ((ConsumerRecord[Array[Byte], Array[Byte]]) => EventData) with Serializable {
  override def apply(v1: ConsumerRecord[Array[Byte], Array[Byte]]): EventData = {
    // Custom conversion code here (e.g. v1.value())
  }
}
```

### Compiling
Create an SBT project to contain your scala object code with package references to both the Kafka and EventHub packages referenced by this project. Relevant snippet below. Package the scala object into a JAR.

#### build.sbt
```scala
// ...

libraryDependencies ++= Seq(
  "com.microsoft.azure" % "azure-eventhubs" % "0.15.1",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion
)
```

### Specifying the adapter
To use the compiled custom adapter, it must first be present in the mirroring tool's classpath. Spark can automatically pull down and include classes from JARs through the use of the `--jars` argument used during cluster submission. There are a [few ways to achieve this](https://spark.apache.org/docs/latest/submitting-applications.html#advanced-dependency-management), but for simplicity, let's assume the packaged JAR is located on HDFS as `hdfs://MyCustomAdapter.jar`.

With the JAR available in HDFS, the following submission command specifies its location to Spark and simultaneously configures the mirroring tool with the adapter's name, `com.example.adapters.MyCustomAdapter`.

```sh
spark-submit --jars hdfs://MyCustomAdapter.jar hdfs://kafka-to-eventhub.jar \
  --duration 5s \
  --broker-list localhost:9092 \
  --zookeeper localhost:2181 \
  --group-id test1 \
  --topics test \
  --adapter-object com.example.adapters.MyCustomAdapter \
  --eh-conn "Endpoint=..." \
  --eh-name kafka-to-eventhub
```

# Advanced Configuration
If you need more control over Spark's connection to Kafka (e.g. SSL configuration), you can define a Scala object which provides a Kafka configuration map containing any key-value pairs documented [here](http://kafka.apache.org/documentation.html#newconsumerconfigs).

The default map provider object (used otherwise) is defined as follows.

```scala
package mn.hart.kafka2eventhub

import org.apache.kafka.common.serialization.ByteArrayDeserializer

object DefaultKafkaParams extends (() => Map[String, Object]) {
  override def apply(): Map[String, Object] = Map(
    "key.deserializer" -> classOf[ByteArrayDeserializer],
    "value.deserializer" -> classOf[ByteArrayDeserializer],
    "auto.offset.reset" -> "earliest",
    "enable.auto.commit" -> false.asInstanceOf[Object]
  )
}
```

Use it as a template to create your own if needed. Note that the tool will attempt to validate these settings to ensure necessary expectations are not violated (such as auto commit remaining disabled). This can be overridden by including the `--force` parameter, but will result in data loss.

## Specifying the configuration map
Just as with custom event adapters, the Kafka configuration map provider object must be packaged and submitted along with the mirroring tool at deployment. Feel free to package it into the same JAR as any custom event adapters you may have.

To specify the configuration map provider object during deployment, include `--kafka-params-object` along with the fully qualified name of the Scala object.

## Deserializers

The deserializers specified in the Kafka configuration map (i.e. `ByteArrayDeserializer`) should correspond to the types of the selected custom event adapter. For example, if `key.deserializer` and `value.deserializer` were both set to `classOf[LongDeserializer]`, the event adapter selected in tandem with this config map (through program arguments) should conform to the following interface.

```scala
object MyLongAdapter extends ((ConsumerRecord[Long, Long]) => EventData) with Serializable {
  override def apply(v1: ConsumerRecord[Long, Long]): EventData = ???
}
```

# Notes
- At-least-once semantics are guaranteed, meaning each event in Kafka will be placed into the EventHub topic at least once, but not exactly once (duplicates are possible). Consequently, downstream EventHub topic consumers must be able to handle duplicates.
- When configuring Spark workers, consider that tasks running in parallel on the same Spark nodes may be competing for network bandwidth.
- Check the tool's help text for supplemental instruction.
