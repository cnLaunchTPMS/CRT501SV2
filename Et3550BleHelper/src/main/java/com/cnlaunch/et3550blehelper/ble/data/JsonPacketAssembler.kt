package com.cnlaunch.et3550blehelper.ble.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TreeMap


class JsonPacketAssembler(
  private val callback :AssemblerCallback
) {

  private var totalPackets = -1
  private val packetMap = TreeMap<Int, ByteArray>()

  interface AssemblerCallback {
    fun onComplete(json: String)
    fun onError(error: String)
  }

  fun reset() {
    totalPackets = -1
    packetMap.clear()
  }

  fun accept(raw: ByteArray) {
    if (raw.size < 4) {
      callback.onError("Packet too short")
      return
    }

    val seq = raw[0].toInt() and 0xFF
    val total = raw[1].toInt() and 0xFF
    val len =
      (raw[2].toInt() and 0xFF) or
      ((raw[3].toInt() and 0xFF) shl 8)

    if (raw.size < 4 + len) {
      callback.onError("DATA_LEN mismatch")
      reset()
      return
    }

    if (totalPackets == -1) {
      totalPackets = total
    }

    if (totalPackets != total) {
      callback.onError("TOTAL mismatch")
      reset()
      return
    }

    packetMap[seq] = raw.copyOfRange(4, 4 + len)

    // 是否收齐
    if (packetMap.size == totalPackets) {
      val mergedSize = packetMap.values.sumOf { it.size }
      val merged = ByteArray(mergedSize)

      var offset = 0
      for (i in 0 until totalPackets) {
        val part = packetMap[i]
        if (part == null) {
          callback.onError("Missing packet seq=$i")
          reset()
          return
        }
        System.arraycopy(part, 0, merged, offset, part.size)
        offset += part.size
      }

      try {
        val json = merged.toString(Charsets.UTF_8)
        callback.onComplete(json)
      } catch (e: Exception) {
        callback.onError("UTF-8 decode failed")
      } finally {
        reset()
      }
    } else {

    }
  }


  companion object{

    private const val CUSTOM_HEADER_SIZE = 4 // 自定义包头大小（SEQ+TOTAL+DATA_LEN）


    /**
     * 分段生成数据包（大数据量使用）
     * @param jsonString 要发送的JSON字符串
     * @param maxSize 也就是MTU大小，单个包最大数据长度
     * @return 数据包列表，每个包都是ByteArray
     */
    fun generatePackets(jsonString: String, maxSize: Int): List<ByteArray> {
      val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
      val totalSize = jsonBytes.size

      // 计算需要多少包
      val totalPackets = if (totalSize % maxSize == 0) {
        totalSize / maxSize
      } else {
        totalSize / maxSize + 1
      }


      val packets = mutableListOf<ByteArray>()

      // 分段生成数据包
      for (seq in 0 until totalPackets) {
        val start = seq * maxSize
        val end = minOf(start + maxSize, totalSize)
        val fragmentSize = end - start

        // 复制当前片段的数据
        val fragmentData = ByteArray(fragmentSize)
        System.arraycopy(jsonBytes, start, fragmentData, 0, fragmentSize)

        // 构建数据包
        val packet = ByteBuffer.allocate(CUSTOM_HEADER_SIZE + fragmentSize)
          .order(ByteOrder.LITTLE_ENDIAN)
          .apply {
            put(seq.toByte())                       // SEQ: 当前包序号
            put(totalPackets.toByte())              // TOTAL: 总包数
            putShort(fragmentSize.toShort())        // DATA_LEN: 片段长度
            put(fragmentData)                       // DATA: JSON片段
          }
          .array()

        packets.add(packet)
      }

      return packets
    }
  }
}
