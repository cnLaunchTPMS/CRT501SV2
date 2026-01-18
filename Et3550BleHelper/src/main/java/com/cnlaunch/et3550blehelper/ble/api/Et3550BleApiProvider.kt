@file:OptIn(DelicateCoroutinesApi::class)

package com.cnlaunch.et3550blehelper.ble.api

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import cn.com.heaton.blelibrary.ble.model.BleDevice
import kotlinx.coroutines.DelicateCoroutinesApi


class Et3550BleApiProvider(private val activity: FragmentActivity) {


  companion object{
    private val TAG = this::class.java.simpleName
    private const val DEVICE_NAME_PREFIX = "BB"
  }

  private val bleViewModel by lazy {
    val vm = ViewModelProvider(activity)[Et3550BleViewModel::class.java].apply {
      initConfig(activity)
    }
    activity.lifecycle.addObserver(vm)
    return@lazy vm
  }


  /**
   * 获取所需权限列表
   */
  private fun getPermissionList(): MutableList<String> {
    val bluetoothPermissionArray = mutableListOf(
      Manifest.permission.BLUETOOTH,
      Manifest.permission.BLUETOOTH_ADMIN,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    )


    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
      bluetoothPermissionArray.addAll(
        arrayOf(
          Manifest.permission.BLUETOOTH_SCAN,
          Manifest.permission.BLUETOOTH_ADVERTISE,
          Manifest.permission.BLUETOOTH_CONNECT
        )
      )
    }

    return bluetoothPermissionArray
  }

  private fun checkPermission(): Boolean {

    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled) {
      return false
    }

    return getPermissionList().all {
      return ContextCompat.checkSelfPermission(activity, it) ==
          PackageManager.PERMISSION_GRANTED
    }


  }


  /**
   * 开始扫描
   * @param callback 回调
   */
  fun startScan(callback: Et3550BleViewModel.Et3550BleScanCallback) {
    if (!checkPermission()) {
      callback.onError("please check bluetooth enable and permissions have be granted :" + getPermissionList().toString())
    }
    bleViewModel.startBleScan(DEVICE_NAME_PREFIX, callback)
  }


  /**
   * 开始链接
   * @param bleDevice 蓝牙类
   * @param callback 回调
   */
  fun startConnect(bleDevice: BleDevice, callback: Et3550BleViewModel.Et3550BleConnectCallback) {
    if (!checkPermission()) {
      callback.onError("please check bluetooth enable and permissions have be granted :" + getPermissionList().toString())
    }
    bleViewModel.startBleConnect(
      bleDevice.bleAddress,
      bleDevice.bleName.replace(DEVICE_NAME_PREFIX, ""),
      callback
    )
  }

  /**
   * 通过序列号和mac地址自动扫描链接
   * @param macNo mac地址
   * @param sn 序列号
   * @param callback 回调
   */
  fun autoScanAndConnectBySnMac(
    macNo: String,
    sn: String,
    callback: Et3550BleViewModel.Et3550BleConnectCallback
  ) {
    if (!checkPermission()) {
      callback.onError("please check bluetooth enable and permissions have be granted :" + getPermissionList().toString())
    }
    bleViewModel.startBleScanConnectBySn(sn,macNo, DEVICE_NAME_PREFIX, callback)
  }



  /**
   * 写入数据
   * @param enumEt3550UUID uuid
   * @param jsonString json数据
   * @param callback 回调
   */
  fun writeData(
    enumEt3550UUID: EnumEt3550UUID,
    jsonString: String,
    callback: Et3550BleViewModel.Et3550BleWriteCallback
  ) {
    bleViewModel.writeJsonData(enumEt3550UUID, jsonString, callback)
  }


  /**
   * 写入数据
   * @param enumEt3550UUID uuid
   * @param callback 回调
   */
  fun readData(
    enumEt3550UUID: EnumEt3550UUID,
    callback: Et3550BleViewModel.Et3550BleReadCallback
  ) {
    bleViewModel.readJsonData(enumEt3550UUID,callback)
  }
}