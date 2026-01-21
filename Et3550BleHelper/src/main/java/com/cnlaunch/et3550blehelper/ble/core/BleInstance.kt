package com.cnlaunch.et3550blehelper.ble.core

import android.content.Context
import android.util.Log
import cn.com.heaton.blelibrary.ble.model.BleDevice
import com.cnlaunch.et3550blehelper.ble.tools.BleToolsLog
import com.cnlaunch.et3550blehelper.ble.data.JsonPacketAssembler
import java.lang.ref.WeakReference

object BleInstance : BleCore() {

  private var hasInit = false

  interface CallbackListenerOuter {
    fun onScan(isScanning: Boolean) {}
    fun onScanCallBack(bleDeviceBean: BleDevice) {}
    fun onConnect(isConnecting: Boolean) {}
    fun onConnected(device: BleDevice) {}
    fun onDisconnected() {}
    fun onData(data: ByteArray?) {}
    fun onData(device: BleDevice,data: ByteArray?) {}
  }

  private var bleDeviceWeakReference: WeakReference<BleDevice> = WeakReference<BleDevice>(null)


  private val listenerOuterMap: MutableMap<String, CallbackListenerOuter> by lazy {
    HashMap()
  }


  /**
   * 初始化
   */
  fun initConfig(
    context: Context,
    uuidService: String = "",
    uuidWriteCha: String = "",
    uuidNotifyCha: String = "",
  ) {
    if (hasInit) {
      return
    }
    super.initBle(context,uuidService, uuidWriteCha, uuidNotifyCha)
    hasInit = true
  }


  /**
   * 是否已连接
   */
  fun checkIsConnected(): Boolean {
    return bleDeviceWeakReference.get()?.isConnected ?: false
  }


  /**
   * 是否在扫描
   */
  fun checkIsScanning() = if (!hasInit) false else instance.isScanning


  /**
   * 注册监听
   * @param tag 标识
   * @param callbackListenerOuter
   */
  fun registerListener(tag: String, callbackListenerOuter: CallbackListenerOuter) {
    listenerOuterMap[tag] = callbackListenerOuter
    bleDeviceWeakReference.get()?.let {
      callbackListenerOuter.onConnected(it)
    }
  }

  /**
   * 注销监听
   * @param tag 标识
   */
  fun unRegisterListener(tag: String) {
    listenerOuterMap.remove(tag)
  }





  /**
   * 扫描
   */
  fun scan(vararg prefixNames : String) = super.startScan(object : ScanCallback {
    override fun start() {
      listenerOuterMap.forEach { listener ->
        listener.value.onScan(true)
      }
    }

    override fun onData(device: BleDevice, byteArray: ByteArray) {
      listenerOuterMap.forEach { listener ->
        listener.value.onData(device,byteArray)
      }
    }

    override fun scanning(bleDeviceBean: BleDevice) {
      listenerOuterMap.forEach { listener ->
        listener.value.onScanCallBack(bleDeviceBean)
      }
    }

    override fun stop() {
      listenerOuterMap.forEach { listener ->
        listener.value.onScan(false)
      }
    }
  },*prefixNames)


  /**
   * 扫描
   */
  fun stopScanOut() = super.stopScan()


  /**
   * 连接
   */
  fun connect(macNo: String, pin: String) {
    super.startConnect(macNo, pin, object : ConnectCallback {
      override fun start() {
        listenerOuterMap.forEach { listener ->
          listener.value.onConnect(true)
        }
      }

      override fun connected(device: BleDevice) {
        bleDeviceWeakReference = WeakReference(device)
        listenerOuterMap.forEach { listener ->
          listener.value.onConnected(device)
        }
      }


      override fun onCallBack(data: ByteArray) {
        listenerOuterMap.forEach { listener ->
          listener.value.onData(data)
        }

      }


      override fun disconnect() {
        bleDeviceWeakReference = WeakReference(null)
        listenerOuterMap.forEach { listener ->
          listener.value.onDisconnected()
        }
      }

      override fun stop() {
        listenerOuterMap.forEach { listener ->
          listener.value.onConnect(false)
        }
      }

    })
  }


  fun getMtuSize(): Int {
    return BLE_MTU
  }

  /**
   * 写数据
   * @param data 数据
   */
  fun write(data: ByteArray, writeCallback: (Boolean) -> Unit) {
    bleDeviceWeakReference.get()?.let { device ->
      super.bleWrite("","",device, data, object : WriteCallBack {
        override fun onCallBack(isSuccess: Boolean) {
          writeCallback.invoke(isSuccess)
        }
      })
    } ?: writeCallback.invoke(false)
  }


  /**
   * 写数据
   * @param data 数据
   */
  fun writeByUUID(
    mainServiceUUID: String,
    writeChaUUID: String,
    data: ByteArray,
    writeCallback: (Boolean) -> Unit
  ) {
    bleDeviceWeakReference.get()?.let { device ->
      super.bleWrite(mainServiceUUID,writeChaUUID, device, data, object : WriteCallBack {
        override fun onCallBack(isSuccess: Boolean) {
          writeCallback.invoke(isSuccess)
        }
      })
    } ?: writeCallback.invoke(false)
  }


  fun readJsonByUUID(
    mainServiceUUID: String,
    readChaUUID: String,
    callBack: (Boolean,ByteArray) -> Unit
  ) {


    bleDeviceWeakReference.get()?.let { device ->
      super.bleRead(mainServiceUUID, readChaUUID, device,callBack)
    } ?: callBack.invoke(false, ByteArray(0))
  }



  /**
   * 释放资源
   */
  fun disconnect() {
    super.release()
    bleDeviceWeakReference = WeakReference(null)
  }




  /**
   * 释放资源
   */
  fun releaseBle() {
    super.release()
    hasInit = false
    bleDeviceWeakReference = WeakReference(null)
    listenerOuterMap.clear()
  }
}