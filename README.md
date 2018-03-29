# Overview
This purpose of this project is to mirror data from a Kafka instance into Azure EventHub in real time. It consists of an Apache Spark streaming application which allows it to scale out to match the parallelism (# of partitions) of the source Kafka topic(s).

To use this tool, build it and deploy it to a Spark cluster. It should work out of the box with minimal configuration. See the deployment section below for a description of available parameters.

# Notes
- At-least-once semantics are guaranteed, meaning each event in Kafka will be placed into the EventHub topic at least once, but not exactly once (duplicates are possible). Consequently, downstream EventHub topic consumers must be able to handle duplicates.
