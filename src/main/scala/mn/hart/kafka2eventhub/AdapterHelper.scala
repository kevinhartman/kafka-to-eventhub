package mn.hart.kafka2eventhub

import scala.util.{Failure, Success, Try}
import scala.reflect.runtime.universe

object AdapterHelper {
  def findAdapterFunction(adapterFunctionClass: String): ((_, _)) => Array[Byte] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

    val companionObj = Try(runtimeMirror.staticModule(adapterFunctionClass)) match {
      case Success(module) =>
        Try(runtimeMirror.reflectModule(module).instance.asInstanceOf[((_, _)) => Array[Byte]]) match {
          case Success(obj) => obj
          case Failure(ex) => throw new Exception(s"Class '$adapterFunctionClass' was found but is not of type 'Function1'.", ex)
        }
      case Failure(ex) => throw new Exception(s"Specified adapter class '$adapterFunctionClass' was not found.", ex)
    }

    companionObj
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
