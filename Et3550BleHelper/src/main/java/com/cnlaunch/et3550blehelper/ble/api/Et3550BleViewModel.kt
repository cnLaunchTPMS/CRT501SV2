package com.cnlaunch.et3550blehelper.ble.api


import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback
import cn.com.heaton.blelibrary.ble.model.BleDevice
import com.cnlaunch.et3550blehelper.ble.core.BleCore
import com.cnlaunch.et3550blehelper.ble.tools.BleToolsLog
import com.cnlaunch.et3550blehelper.ble.core.BleInstance
import com.cnlaunch.et3550blehelper.ble.data.JsonPacketAssembler
import com.cnlaunch.et3550blehelper.ble.data.JsonPacketAssembler.Companion.generatePackets
import com.cnlaunch.et3550blehelper.ble.tools.BleTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.UUID


@DelicateCoroutinesApi
class Et3550BleViewModel : ViewModel(), DefaultLifecycleObserver {


  companion object{
    private val TAG = this::class.java.simpleName
  }



  private val tagForScan = this.hashCode().toString() + "tag_for_scan"

  private val tagForConnect = this.hashCode().toString() + "tag_for_connect"

  private val tagForMix = this.hashCode().toString() + "tag_for_mix"


  private var mainUUID: String = EnumEt3550UUID.UUID_PRIMARY_SERVICE.value

  private val scopeInner: CoroutineScope by lazy {
    CoroutineScope(SupervisorJob() + newSingleThreadContext(Et3550BleViewModel::class.java.simpleName))
  }

  interface Et3550BleScanCallback {
    fun onScanningStatusChanged(isScanning: Boolean)
    fun onFindOutDevice(device: BleDevice?)
    fun onError(msg: String)
  }

  interface Et3550BleConnectCallback{
    fun onLoadingStatusChanged(isLoading: Boolean)
    fun onConnectStatusChanged(isConnected: Boolean)
    fun onNotifyData(data: ByteArray)
    fun onError(msg: String)
  }

  interface Et3550BleWriteCallback{
    fun onLoadingStatusChanged(isLoading: Boolean)
    fun onWriteResult(success: Boolean,errorMsg: String)
  }

  interface Et3550BleReadCallback{
    fun onLoadingStatusChanged(isLoading: Boolean)
    fun onReadResult(success: Boolean,json: String,errorMsg: String)
  }


  internal fun initConfig(context: Context,uuidMainServiceString : String) {
    BleInstance.initConfig(context, uuidMainServiceString, "","");
  }


  internal fun checkIsConnected(mac: String): Boolean {
    return BleInstance.checkIsConnected(mac)
  }


  /**
   * 蓝牙扫描
   */
  internal fun startBleScan(prefixName : String,callback: Et3550BleScanCallback) {


    BleInstance.registerListener(
      tagForScan, object : BleInstance.CallbackListenerOuter {
        override fun onScan(isScanning: Boolean) {
          scopeInner.launch(Dispatchers.Main) {
            callback.onScanningStatusChanged(isScanning)
          }
        }

        override fun onData(device: BleDevice, data: ByteArray?) {
          super.onData(device, data)
          if (device.bleName.startsWith(prefixName) && data != null
            && getDeviceNameCheck(prefixName, device.bleName, data)
            && BleInstance.checkIsScanning()
          ) {
            scopeInner.launch(Dispatchers.Main) {
              callback.onFindOutDevice(device)
            }
          }
        }

      })
    callback.onFindOutDevice(null)
    BleInstance.scan(prefixName)
  }


  /**
   * 蓝牙直接连接
   * @param macNo mac地址
   * @param pin pin码
   * @param callback 回调
   */
  internal fun startBleConnect(macNo: String, pin: String, callback: Et3550BleConnectCallback) {
    BleInstance.registerListener(tagForConnect, object : BleInstance.CallbackListenerOuter {
      override fun onConnect(isConnecting: Boolean) {
        if (!isConnecting) {
          scopeInner.launch(Dispatchers.Main) {
            callback.onLoadingStatusChanged(true)
          }
        }
      }

      override fun onData(data: ByteArray?) {
        super.onData(data)
        scopeInner.launch(Dispatchers.Main) {
          if (data != null) {
            callback.onNotifyData(data)
          }
        }
      }


      override fun onConnected(device: BleDevice) {
        super.onConnected(device)
        scopeInner.launch(Dispatchers.Main) {
          callback.onConnectStatusChanged(true)
          callback.onLoadingStatusChanged(false)
        }
      }

      override fun onDisconnected() {
        super.onDisconnected()
        scopeInner.launch(Dispatchers.Main) {
          callback.onConnectStatusChanged(false)
          callback.onLoadingStatusChanged(false)
        }
      }
    })
    BleInstance.connect(macNo,pin)
  }


  /**
   * 通过设备sn号进行蓝牙扫描连接
   * @param sn sn号
   * @param mac Mac地址
   * @param callback 连接回调
   */
  internal fun startBleScanConnectBySn(
    sn: String,
    mac: String,
    prefixName: String,
    callback: Et3550BleConnectCallback
  ) {

    BleInstance.registerListener(tagForMix, object : BleInstance.CallbackListenerOuter {
      override fun onScanCallBack(bleDeviceBean: BleDevice) {
        if (bleDeviceBean.bleName.equals(sn)
          && bleDeviceBean.bleAddress.equals(mac, true)
          && BleInstance.checkIsScanning()
        ) {
          BleInstance.connect(
            bleDeviceBean.bleAddress,
            bleDeviceBean.bleName.replace(prefixName, ""),
          )
        }
      }

      override fun onData(data: ByteArray?) {
        super.onData(data)
        scopeInner.launch(Dispatchers.Main) {
          if (data != null) {
            callback.onNotifyData(data)
          }
        }
      }



      override fun onConnect(isConnecting: Boolean) {
        if (isConnecting) {
          scopeInner.launch(Dispatchers.Main) {
            callback.onLoadingStatusChanged(true)
          }
        }
      }

      override fun onConnected(device: BleDevice) {
        super.onConnected(device)
        scopeInner.launch(Dispatchers.Main) {
          callback.onConnectStatusChanged(true)
          callback.onLoadingStatusChanged(false)
        }
      }
      
      
      override fun onDisconnected() {
        scopeInner.launch(Dispatchers.Main) {
          callback.onConnectStatusChanged(false)
          callback.onLoadingStatusChanged(false)
        }
      }
    })
    BleInstance.scan(prefixName)
  }



  /**
   * 写入数据
   * @param data 数据
   * @param callback 回调
   */
  internal fun writeDataDirectly(
    characteristicUUID: EnumEt3550UUID,
    data: ByteArray,
    callback: Et3550BleWriteCallback
  ) {
    scopeInner.launch (Dispatchers.IO) {
      callback.onLoadingStatusChanged(true)
      BleInstance.writeByUUID(this@Et3550BleViewModel.mainUUID,characteristicUUID.value,data) { success ->
        callback.onLoadingStatusChanged(success)
        callback.onWriteResult(success,"")
      }
    }
  }


  /**
   * 写入蓝牙数据（自动分包、顺序写、汇总结果）
   * @param json JSON字符串
   * @param callback 回调
   */
  internal fun writeJsonData(
    characteristicUUID: EnumEt3550UUID,
    json: String,
    callback: Et3550BleWriteCallback
  ) {
    scopeInner.launch (Dispatchers.IO) {
      callback.onLoadingStatusChanged(true)
      val mtu = BleInstance.getMtuSize()
      val packets = generatePackets(json, mtu)
      if (packets.isEmpty()) {
        BleToolsLog.d(TAG, "writJsonData 数据为空")
        callback.onWriteResult(false,"write data is empty")
        callback.onLoadingStatusChanged(false)
        return@launch
      }
      submitPacketsSequentially(
        mainUUID = this@Et3550BleViewModel.mainUUID,
        characteristicUUID = characteristicUUID.value,
        packets = packets,
        index = 0,
        onResult = {
          scopeInner.launch(Dispatchers.Main) {
            callback.onLoadingStatusChanged(false)
            callback.onWriteResult(it,"")
          }
        }
      )
    }
  }


  /**
   * 读取数据
   * @param characteristicUUID 特征值
   * @param callback 回调
   */
  internal fun readJsonData(characteristicUUID: EnumEt3550UUID, callback: Et3550BleReadCallback) {
    scopeInner.launch(Dispatchers.IO) {
      callback.onLoadingStatusChanged(true)
      writeJsonData(
        EnumEt3550UUID.UUID_READ_CONTROL,
        "{\"RDC\":\"${characteristicUUID.key}\"}",
        object : Et3550BleWriteCallback {
          override fun onLoadingStatusChanged(isLoading: Boolean) {
            callback.onLoadingStatusChanged(isLoading)
          }

          override fun onWriteResult(success: Boolean, errorMsg: String) {
            if (success) {
              scopeInner.launch(Dispatchers.IO) {
                delay(1000)



                val finished = java.util.concurrent.atomic.AtomicBoolean(false)

                val assembler = JsonPacketAssembler(object : JsonPacketAssembler.AssemblerCallback {
                  override fun onComplete(json: String) {
                    if (finished.compareAndSet(false, true)) {
                      scopeInner.launch(Dispatchers.Main) {
                        callback.onLoadingStatusChanged(false)
                        callback.onReadResult(true, json, "")
                      }
                    }
                  }

                  override fun onError(error: String) {
                    if (finished.compareAndSet(false, true)) {
                      scopeInner.launch(Dispatchers.Main) {
                        callback.onLoadingStatusChanged(false)
                        callback.onReadResult(false, "", error)
                      }
                    }
                  }
                })

                scopeInner.launch(Dispatchers.IO) {
                  // 最多防御性读取 300 次 30000 ms，避免死循环
                  repeat(300) {
                    if (finished.get()) {
                      return@launch
                    }

                    BleToolsLog.e(TAG, "读取未完成，继续读取... 第${it + 1}包")

                    BleInstance.readJsonByUUID(
                      mainUUID,
                      characteristicUUID.value
                    ){ success, data ->
                      if (success && data.isNotEmpty()) {
                        assembler.accept(data)
                      }else if (!success){
                        BleToolsLog.e(TAG, "readJsonByUUID read failed")
                        if (finished.compareAndSet(false, true)) {
                          scopeInner.launch(Dispatchers.Main) {
                            callback.onLoadingStatusChanged(false)
                            callback.onReadResult(false, "", "read json failed")
                          }
                        }
                        this.cancel()
                      }
                    }

                    delay(100)
                  }

                  // 超时仍未完成
                  if (finished.compareAndSet(false, true)) {
                    scopeInner.launch(Dispatchers.Main) {
                      callback.onLoadingStatusChanged(false)
                      callback.onReadResult(false, "", "read json timeout")
                    }
                  }
                }



              }
            }else{
              scopeInner.launch(Dispatchers.Main) {
                callback.onLoadingStatusChanged(false)
                callback.onReadResult(false, "", errorMsg)
              }
            }
          }
        }
      )
    }
  }





  private fun submitPacketsSequentially(
    mainUUID: String = "",
    characteristicUUID: String = "",
    packets: List<ByteArray>,
    index: Int,
    onResult: (Boolean) -> Unit
  ) {
    // 所有包写完
    if (index >= packets.size) {
      onResult(true)
      return
    }

    BleInstance.writeByUUID(mainUUID,characteristicUUID,packets[index]) { success ->
      if (!success) {
        // 任意一包失败，整体失败
        BleToolsLog.e("BLE_WRITE", "packet[$index] write failed")
        onResult(false)
        return@writeByUUID
      }

      // 当前包成功，继续写下一个
      submitPacketsSequentially(
        packets = packets,
        index = index + 1,
        onResult = onResult
      )
    }
  }


  /**
   * 解析蓝牙广播数据，查找符合规范的设备名称
   */
  private fun getDeviceNameCheck(
    prefixName: String,
    nameCheck: String,
    scanRecord: ByteArray
  ): Boolean {
    var offset = 0

    while (offset < scanRecord.size) {
      // 读取AD结构的长度（不包括长度字节本身）
      val length = scanRecord[offset].toInt() and 0xFF
      if (length == 0) break

      // 检查是否有足够的字节
      if (offset + length >= scanRecord.size) break

      val adType = scanRecord[offset + 1].toInt() and 0xFF

      // 如果是Complete Local Name (0x08)
      if (adType == 0x08) {
        // AD数据的起始位置（跳过长度和类型字节）
        val dataStart = offset + 2
        val dataLength = length - 1  // 减去AD Type字节

        // 检查数据长度是否符合预期（8个字符）
        if (dataLength == 8) {
          val nameBytes = scanRecord.copyOfRange(dataStart, dataStart + dataLength)
          val name = String(nameBytes, Charsets.UTF_8)

          // 验证名称是否符合规范
          if (name.startsWith(prefixName)) {

            return nameCheck.equals(name, true)
          }
        }
      }

      // 移动到下一个AD结构
      offset += length + 1
    }

    return false
  }


  internal fun stopScan() {
    BleInstance.stopScanOut()
  }


  internal fun releaseBle() {
    BleInstance.stopScanOut()
    BleInstance.disconnect()
    BleInstance.releaseBle()
    BleInstance.unRegisterListener(tagForScan)
    BleInstance.unRegisterListener(tagForMix)
    BleInstance.unRegisterListener(tagForConnect)
    scopeInner.cancel()
  }

  override fun onCleared() {
    super.onCleared()
    releaseBle()
  }
  
}