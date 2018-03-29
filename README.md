# Overview
This purpose of this project is to mirror data from a Kafka instance into Azure EventHub in real time. It consists of an Apache Spark streaming application which allows it to scale out to match the parallelism (# of partitions) of the source Kafka topic(s).

To use this tool, build it and deploy it to a Spark cluster. It should work out of the box with minimal configuration. See the deployment section below for a description of available parameters.

# Events
By default, Kafka messages (records) are transformed into EventHub events simply by dropping the message's key. In other words, the value of the Kafka message is forwarded to EventHub verbatim, while the key is ignored. Recall that Kafka messages are key-value pairs, while EventHub events are simply values.

If you need to transform Kafka messages differently, use a custom event adapter as described below. As an example, perhaps you'd like to combine the Kafka message's key and value into a JSON object, sending that along to EventHub.

## Custom event adapters
To define a custom transformation funciton to convert Kafka messages into EventHub events, define a custom event adapter. This is done by writing Scala code, and including the compiled class on the classpath when submitting the `kafka-to-eventhub` tool to the Spark cluster.

### Code example
To implement a custom adapter, use the following as a template to define a root-level Scala object. See the [DefaultAdapter](src/main/scala/mn/hart/kafka2eventhub/DefaultAdapter.scala) for an example. Note that the package name is unimportant.

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
To use the compiled custom adapter, it must first be present in the Spark application's classpath so that it's found. Spark can do this automatically through the use of the `--jars` argument used during cluster submission. There are a [few ways to achieve this](https://spark.apache.org/docs/latest/submitting-applications.html#advanced-dependency-management), but for simplicity, let's assume the packaged JAR is located on HDFS as `hdfs://MyCustomAdapter.jar`.

With the JAR available in HDFS, the following submission command specifies its location to Spark and simultaneously configures the tool with the adapter's name, `com.example.adapters.MyCustomAdapter`.

```sh
spark-submit --jars hdfs://MyCustomAdaper.jar hdfs://kafka-to-eventhub.jar \
  --duration 5s \
  --broker-list localhost:9092 \
  --zookeeper localhost:2181 \
  --group-id test1 \
  --topics test \
  --adapter-object com.example.adapters.MyCustomAdapter \
  --eh-conn "Endpoint=..." \
  --eh-name kafka-to-eventhub
```

# Notes
- At-least-once semantics are guaranteed, meaning each event in Kafka will be placed into the EventHub topic at least once, but not exactly once (duplicates are possible). Consequently, downstream EventHub topic consumers must be able to handle duplicates.
