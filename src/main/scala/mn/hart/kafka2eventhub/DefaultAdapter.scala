package mn.hart.kafka2eventhub

object DefaultAdapter extends (((Array[Byte], Array[Byte])) => Array[Byte]) {
  override def apply(v1: (Array[Byte], Array[Byte])): Array[Byte] = v1._2
}