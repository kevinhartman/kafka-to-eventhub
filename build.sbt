name := "kafka-to-eventhub"
scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-feature"
)

val sparkVersion = "2.2.1"

// Dependencies provided by the Spark clusters
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion
)

// Bundled dependencies
libraryDependencies ++= Seq(
  "com.microsoft.azure" % "azure-eventhubs" % "0.15.1",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "com.microsoft.azure" % "azure-eventhubs" % "0.15.1",
  "com.github.scopt" % "scopt_2.11" % "3.7.0"
)
