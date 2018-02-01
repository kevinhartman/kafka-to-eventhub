package mn.hart.kafka2eventhub

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.GZIPOutputStream

import com.google.common.io.ByteStreams
import com.microsoft.azure.eventhubs.EventData
import org.apache.spark.rdd.RDD

object GZipCompress {
  def apply(rdd: RDD[EventData]): RDD[EventData] = rdd.map(data => {
    val bytes = data.getBytes
    val sourceStream = new ByteArrayInputStream(bytes)
    val outputStream = new ByteArrayOutputStream((bytes.length * 3) / 5) // pre-alloc assuming 60% of original size

    var gzip: GZIPOutputStream = null
    try {
      gzip = new GZIPOutputStream(outputStream)
      ByteStreams.copy(sourceStream, gzip)
    } finally if (gzip != null) {
      gzip.close()
    }

    val compressedData = new EventData(outputStream.toByteArray)
    compressedData.setProperties(data.getProperties)

    compressedData
  })
}
