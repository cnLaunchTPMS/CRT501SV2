package com.cnlaunch.et3550blehelper.ble.tools

import org.json.JSONObject
import java.nio.charset.Charset

internal object BleTools {

  fun bytesToHexString(byteArray: ByteArray?) : String {
    val stringBuilder = StringBuilder()
    if (byteArray == null || byteArray.isEmpty()) {
      return ""
    }
    for (b in byteArray) {
      val hex = String.format("%02X", b)
      stringBuilder.append(hex)
    }
    return stringBuilder.toString().trim()
  }



  fun stringToByteArray(text: String, charset: Charset = Charsets.UTF_8): ByteArray {
    return text.toByteArray(charset)
  }


  fun bytesToJson(data: ByteArray?): JSONObject? {
    if (data == null || data.isEmpty()){
      return null
    }
    return try {
      val jsonString = String(data, Charsets.UTF_8).trim()
      JSONObject(jsonString)
    } catch (e: Exception) {
      println("转换失败: ${e.message}")
      null
    }
  }

}