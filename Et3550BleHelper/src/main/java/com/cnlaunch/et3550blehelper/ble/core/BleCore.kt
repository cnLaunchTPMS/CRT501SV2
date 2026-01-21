package com.cnlaunch.et3550blehelper.ble.core

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import cn.com.heaton.blelibrary.ble.Ble
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback
import cn.com.heaton.blelibrary.ble.model.BleDevice
import com.cnlaunch.et3550blehelper.ble.tools.BleTools
import com.cnlaunch.et3550blehelper.ble.tools.BleTools.bytesToHexString
import com.cnlaunch.et3550blehelper.ble.tools.BleToolsLog
import java.util.UUID
import kotlin.jvm.internal.Intrinsics


open class BleCore {

  companion object {

    //蓝牙扫描超时时间
    private const val BLE_SCAN_TIME_OUT = 60 * 1000L

    //蓝牙连接超时时间
    private const val BLE_CONNECT_TIME_OUT = 20 * 1000L

    //配对超时时间
    private const val BLE_PAIRING_TIMEOUT = 30 * 1000L

    //蓝牙最大连接数
    private const val BLE_CONNECT_COUNT_MAX = 7

    //蓝牙连接重试次数
    private const val BLE_RECONNECT_COUNT = 3



    //MTU
    var BLE_MTU = 240
    
    //当前配对密码
    var currentPinPassword = ""

    //单例
    internal val instance: Ble<BleDevice> by lazy {
      Ble.getInstance()
    }

  }


  
  private var pairingReceiver: BroadcastReceiver? = null

  //TAG
  private val TAG = this::class.java.simpleName



  protected interface ScanCallback {
    fun start()
    fun onData(device: BleDevice, byteArray: ByteArray)
    fun scanning(bleDeviceBean: BleDevice)
    fun stop()
  }

  protected interface ConnectCallback {
    fun start()
    fun connected(device: BleDevice)
    fun onCallBack(data: ByteArray)
    fun disconnect()
    fun stop()
  }

  protected interface WriteCallBack {
    fun onCallBack(isSuccess: Boolean)
  }

  protected interface ReadCallBack {
    fun onCallBack(isSuccess: Boolean,data: ByteArray)
  }

  protected fun initBle(
    context: Context,
    uuidService : String = "",
    uuidWriteCha : String = "",
    uuidNotifyCha : String = "",
  ) {


    //开启配置
    Ble.options()
      .apply {
        //主服务特征值
        if (uuidService.isNotEmpty()) {
          setUuidService(UUID.fromString(uuidService))
        }
        if (uuidWriteCha.isNotEmpty()) {
          //写特征值
          setUuidWriteCha(UUID.fromString(uuidWriteCha))
        }
        if (uuidNotifyCha.isNotEmpty()) {
          //通知特征值
          setUuidNotifyCha(UUID.fromString(uuidNotifyCha))
        }
      }
      //设置是否输出打印蓝牙日志（非正式打包请设置为true，以便于调试）
      .setLogBleEnable(true)
      //设置是否抛出蓝牙异常
      .setThrowBleException(true)
      //设置是否自动连接
      .setAutoConnect(false)
      //设置是否过滤扫描到的设备(已扫描到的不会再次扫描)
      .setIgnoreRepeat(false)
      //连接失败重试次数
      .setConnectFailedRetryCount(BLE_RECONNECT_COUNT)
      //设置连接超时时长
      .setConnectTimeout(BLE_CONNECT_TIME_OUT)
      //最大连接数量
      .setMaxConnectNum(BLE_CONNECT_COUNT_MAX)
      //设置扫描时长
      .setScanPeriod(BLE_SCAN_TIME_OUT)
      //初始化
      .create<BleDevice>(context, object : Ble.InitCallback {
        override fun success() {
          BleToolsLog.e(this@BleCore.javaClass.simpleName, "初始化成功")
        }

        override fun failed(failedCode: Int) {
          BleToolsLog.e(this@BleCore.javaClass.simpleName, "初始化失败 code $failedCode")
        }
      })
  }


  private fun registerGlobalPairingReceiver(context: Context) {
    if (pairingReceiver != null) {
      // 已经注册过，避免重复注册
      return
    }

    pairingReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_PAIRING_REQUEST) {
          val device = intent.getParcelableExtra<BluetoothDevice>(
            BluetoothDevice.EXTRA_DEVICE
          )
          val variant = intent.getIntExtra(
            BluetoothDevice.EXTRA_PAIRING_VARIANT,
            -1
          )
          val bluetoothDevice =
            intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE") as BluetoothDevice?
          BleToolsLog.d(
            TAG,
            "全局配对请求 - 设备: ${device?.address}, 类型: $variant  设备是否为空： ${bluetoothDevice == null}"
          )

          try {
            if (variant != 0) {
              if (variant == 1) {
                bluetoothDevice?.setPairingConfirmation(true)
                abortBroadcast()
                return
              } else if (variant != 2) {
                return
              }
            }
            if (bluetoothDevice != null) {
              val bytes: ByteArray = BleTools.stringToByteArray(currentPinPassword)
              Intrinsics.checkNotNullExpressionValue(bytes, "this as java.lang.String.getBytes(charset)")
              bluetoothDevice.setPin(bytes)
            }
            abortBroadcast()
          } catch (e: java.lang.Exception) {
            Log.e(TAG, "Failed to set PIN", e)
          }


        }
      }
    }

    val filter = IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
    try {
      filter.priority = 1000  // 尝试高优先级
    } catch (e: Exception) {
      BleToolsLog.d(TAG, "无法设置高优先级")
    }

    context.registerReceiver(pairingReceiver, filter)
    BleToolsLog.d(TAG, "全局配对请求接收器已注册")
  }

  
  fun unregisterGlobalPairingReceiver(context: Context) {
    if (pairingReceiver != null) {
      context.unregisterReceiver(pairingReceiver)
      pairingReceiver = null
      BleToolsLog.d(TAG, "全局配对请求接收器已注销")
    }
  }


  /**
   * 开始扫描
   * @param prefixName 前缀
   * @param callback 外部回调
   */
  protected open fun startScan(callback: ScanCallback, vararg prefixName: String) {
    if (instance.isScanning) {
      instance.stopScan()
    }
    Ble.getInstance<BleDevice>().startScan(object : BleScanCallback<BleDevice>() {
      override fun onLeScan(device: BleDevice?, rssi: Int, scanRecord: ByteArray?) {
        device?.bleName?.let {
          BleToolsLog.d(
            TAG,
            "扫描到设备 ${device.bleName}  ${device.bleAddress}\n${
              scanRecord?.let {
                bytesToHexString(it)
              }
            } \n\n"
          )
          prefixName.forEach {
            if (device.bleName.startsWith(it, true) || device.bleName.contains(it, true)) {
              callback.scanning(device)
              scanRecord?.let { data ->
                callback.onData(device, data)
              }
            }
          }

        }
      }

      override fun onStart() {
        super.onStart()
        callback.start()
        BleToolsLog.d(TAG, "开始扫描")
      }

      override fun onStop() {
        super.onStop()
        callback.stop()
        BleToolsLog.d(TAG, "停止扫描")
      }

      override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        callback.stop()
        BleToolsLog.e(TAG, "扫描异常 errorCode ： $errorCode")
      }
    })

  }



  /**
   * 停止扫描
   */
  protected open fun stopScan() {
    instance.stopScan()
  }


  /**
   * 开始连接
   * @param callback 外部回调
   */
  protected fun startConnect(
    macNo: String,
    pin: String = "",
    callback: ConnectCallback
  ) {

    instance.stopScan()

    currentPinPassword = pin

    registerGlobalPairingReceiver(Ble.getInstance<BleDevice>().context)
    instance.connect(macNo, object : BleConnectCallback<BleDevice>() {
      override fun onReady(device: BleDevice?) {
        super.onReady(device)
        device?.let {
          BleToolsLog.d(TAG, "连接 Ready  $device")
          if (it.isConnected) {

          } else {
            callback.stop()
          }
        }
      }


      override fun onConnectionChanged(device: BleDevice?) {
        device?.let {
          if (it.isDisconnected) {
            BleToolsLog.d(TAG, "连接 Disconnected")
            callback.disconnect()
          }
        }
      }

      override fun onConnectCancel(device: BleDevice?) {
        super.onConnectCancel(device)
        callback.stop()
        device?.let {
          BleToolsLog.d(TAG, "连接 Cancel  ${it.bleName}")
        }

        unregisterGlobalPairingReceiver(Ble.getInstance<BleDevice>().context)
      }


      override fun onServicesDiscovered(device: BleDevice?, gatt: BluetoothGatt?) {
        super.onServicesDiscovered(device, gatt)
        device?.let {
          BleToolsLog.d(TAG, "连接 Discovered  ${it.bleName}")
          Ble.getInstance<BleDevice>().setMTU(device.bleAddress, BLE_MTU, object :
            BleMtuCallback<BleDevice>() {
            override fun onMtuChanged(device: BleDevice?, mtu: Int, status: Int) {
              super.onMtuChanged(device, mtu, status)
              BleToolsLog.d(TAG, "设置MTU : $mtu  status = $status")
              if (mtu != BLE_MTU + 10) {
                BLE_MTU = mtu - 10
              }

              switchNotify(it, true, callback) {
                callback.connected(it)
              }

            }
          })
        }
      }

      override fun onConnectFailed(device: BleDevice?, errorCode: Int) {
        super.onConnectFailed(device, errorCode)
        callback.stop()
        device?.let {
          BleToolsLog.e(TAG, "连接 Failed  $device  code： $errorCode")
          unregisterGlobalPairingReceiver(Ble.getInstance<BleDevice>().context)
        }
      }




    })
  }


  /**
   * 使能通知
   * @param device BleDevice
   * @param isOpen 是否打开通知
   */
  protected fun switchNotify(
    device: BleDevice,
    isOpen: Boolean,
    callback: ConnectCallback,
    notifySuccessListener: () -> Unit
  ) {
    BleToolsLog.d(TAG, "使能通知 ${device.bleName}")
    instance.enableNotify(device,isOpen,object : BleNotifyCallback<BleDevice>() {


      override fun onChanged(device: BleDevice, characteristic: BluetoothGattCharacteristic) {
        characteristic.value?.let {

          BleToolsLog.d(TAG, "Notify Changed")
          BleToolsLog.d(TAG, "Notify Changed data: ${bytesToHexString(it)}")
          callback.onCallBack(it)
        }
      }

      override fun onNotifySuccess(device: BleDevice) {
        super.onNotifySuccess(device)
        BleToolsLog.d(TAG, "Notify Success: ${device.bleName}")
        notifySuccessListener.invoke()
      }

      override fun onNotifyFailed(device: BleDevice?, failedCode: Int) {
        super.onNotifyFailed(device, failedCode)
        callback.stop()
        device?.let {
          BleToolsLog.d(TAG, "Notify Failed  ${it.bleName}  code： $failedCode")
        }
      }

      override fun onNotifyCanceled(device: BleDevice?) {
        super.onNotifyCanceled(device)
        callback.stop()
        device?.let {
          BleToolsLog.d(TAG, "Notify Cancel  $device  code：${it.bleName}")
        }
      }


    })
  }


  protected fun bleRead(
    mainServiceUUID: String = "",
    readChaUUID: String = "",
    deviceIn: BleDevice,
    readCallBack: (Boolean, ByteArray) -> Unit
  ) {
    BleToolsLog.d(TAG, "开始读取")
    if (mainServiceUUID.isNotEmpty() && readChaUUID.isNotEmpty()) {
      instance.readByUuid(
        deviceIn,
        UUID.fromString(mainServiceUUID),
        UUID.fromString(readChaUUID), object : BleReadCallback<BleDevice>() {
          override fun onReadSuccess(dedvice: BleDevice?, characteristic: BluetoothGattCharacteristic?) {

            if (characteristic != null && characteristic.value != null){
              BleToolsLog.d(TAG, "readByUuid() 读取成功：${bytesToHexString(characteristic.value)}")
              readCallBack.invoke(true,characteristic.value)
            }
          }

          override fun onReadFailed(device: BleDevice?, failedCode: Int) {
            super.onReadFailed(device, failedCode)
            BleToolsLog.e(TAG, "读取失败：$failedCode")
            readCallBack.invoke(false, byteArrayOf())
          }
        }
      )
    } else {
      instance.read(deviceIn,object : BleReadCallback<BleDevice>() {
        override fun onReadSuccess(dedvice: BleDevice?, characteristic: BluetoothGattCharacteristic?) {

          if (characteristic != null && characteristic.value != null){
            BleToolsLog.d(TAG, "读取成功：${bytesToHexString(characteristic.value)}")
            readCallBack.invoke(true,characteristic.value)
          }
        }

        override fun onReadFailed(device: BleDevice?, failedCode: Int) {
          super.onReadFailed(device, failedCode)
          BleToolsLog.e(TAG, "读取失败：$failedCode")
          readCallBack.invoke(false, byteArrayOf())
        }
      })
    }
  }




  /**
   * 写
   * @param deviceIn BleDevice
   * @param byteArray 数据
   */
  protected fun bleWrite(
    mainServiceUUID: String = "",
    writeChaUUID: String = "",
    deviceIn: BleDevice,
    byteArray: ByteArray,
    writeCallBack: WriteCallBack
  ) {
    try {
      val shortCallback = object : BleWriteCallback<BleDevice>() {
        override fun onWriteSuccess(
          device: BleDevice?, characteristic: BluetoothGattCharacteristic?
        ) {
          device?.let {
            if (byteArray.contentEquals(characteristic?.value)) {
              writeCallBack.onCallBack(true)
            }
            BleToolsLog.d(TAG, "Write Success  ${device.bleName}   ${bytesToHexString(characteristic?.value)}")
          }

        }

        override fun onWriteFailed(device: BleDevice?, failedCode: Int) {
          super.onWriteFailed(device, failedCode)
          writeCallBack.onCallBack(false)
          device?.let {
            BleToolsLog.e(TAG, "Write Fail ${device.bleName} code: $failedCode")
          }
        }
      }


      if (mainServiceUUID.isNotEmpty() && writeChaUUID.isNotEmpty()) {


        BleToolsLog.d(TAG, "开始写入byUUID")
        instance.writeByUuid(
          deviceIn,
          byteArray,
          UUID.fromString(mainServiceUUID),
          UUID.fromString(writeChaUUID), shortCallback
        )
      } else {
        instance.write(deviceIn, byteArray, shortCallback)
      }

    } catch (e: Exception) {
      BleToolsLog.e(TAG, "写入异常： $e")
      writeCallBack.onCallBack(false)
    }
  }



  protected fun release() {
    currentPinPassword = ""
    instance.context?.unregisterReceiver(pairingReceiver)
    instance.released()
  }




}