package mn.hart.kafka2eventhub

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}
import com.microsoft.azure.eventhubs.{ClientConstants, EventHubClient => MSEventHubClient, RetryExponential}

object EventHubClient {
  val EHFactoryRetryPolicy = new RetryExponential(ClientConstants.DEFAULT_RETRY_MIN_BACKOFF,
    ClientConstants.DEFAULT_RETRY_MAX_BACKOFF, 30, "EventHubClient")

  /**
    * Client cache. Each Spark worker JVM process will have its own cache which will stick around
    * for the lifetime of the process. In this way, multiple executors which run on the same worker will share
    * the EventHub connection instance.
    */
  private val clientCache = new ConcurrentHashMap[String, MSEventHubClient]()

  /**
    * Retrieves an EventHub client / connection for the given connection string. If the client already exists,
    * the existing client is returned, otherwise a new client is created and cached for future calls.
    * @param connStr The connection string for the target EventHub.
    * @return The EventHub client instance which may be used to communicate with the corresponding EventHub.
    */
  def apply(connStr: String): MSEventHubClient = {
    val getClient = new JFunction[String, MSEventHubClient] {
      override def apply(s: String): MSEventHubClient =
        MSEventHubClient.createFromConnectionStringSync(s, EHFactoryRetryPolicy)
    }

    clientCache.computeIfAbsent(connStr, getClient)
  }
}