package mn.hart.kafka2eventhub

import com.microsoft.azure.eventhubs.EventData
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.util.{Failure, Success, Try}
import scala.reflect.runtime.universe

object AdapterHelper {
  def findAdapterFunction(adapterFunctionClass: String): (ConsumerRecord[AnyVal, AnyVal]) => EventData = {
    val adapterFunction = findCompanionObject[(ConsumerRecord[AnyVal, AnyVal]) => EventData](adapterFunctionClass)

    if (!adapterFunction.getClass.getInterfaces.contains(classOf[Serializable])) {
      throw new Exception(s"Class '$adapterFunctionClass' was found but is not serializable.")
    }

    adapterFunction
  }

  def findKafkaParams(kafkaParamsClass: String): Map[String, Object] =
    findCompanionObject[() => Map[String, Object]](kafkaParamsClass)()

  private def findCompanionObject[TCompanion](className: String): TCompanion = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

    val companionObj = Try(runtimeMirror.staticModule(className)) match {
      case Success(module) =>
        Option(runtimeMirror.reflectModule(module).instance) match {
          case Some(instance) => instance.asInstanceOf[TCompanion]
          case None => throw new Exception(s"Class '$className' was found but has no companion object.")
        }
      case Failure(ex) => throw new Exception(s"Specified class '$className' was not found.", ex)
    }

    companionObj
  }
}
