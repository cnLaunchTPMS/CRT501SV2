package com.cnlaunch.crt501sv2util.bean

data class TpmsInitBean(
  var result :Boolean,
  var msg:String,
  var deviceInfoBean: TpmsDeviceInfoBean
){

  override fun toString(): String {
    return "TpmsInitBean(result=$result, msg='$msg', deviceInfoBean=$deviceInfoBean)"
  }
}
