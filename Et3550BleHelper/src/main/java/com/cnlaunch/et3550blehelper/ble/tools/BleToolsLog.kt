package com.cnlaunch.et3550blehelper.ble.tools

internal object BleToolsLog {
  private const val TAG = "Et3550BleHelper"

  fun d(message: String) {
    android.util.Log.d(TAG, message)
  }

  fun d(tag: String, message: String) {
    android.util.Log.d("$TAG-$tag", message)
  }

  fun e(message: String) {
    android.util.Log.e(TAG, message)
  }

  fun e(tag: String, message: String) {
    android.util.Log.e("$TAG-$tag", message)
  }

}