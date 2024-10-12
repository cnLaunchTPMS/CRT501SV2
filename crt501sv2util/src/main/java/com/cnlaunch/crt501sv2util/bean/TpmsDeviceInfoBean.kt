package com.cnlaunch.crt501sv2util.bean

data class TpmsDeviceInfoBean(
  var serialNo: String = "",
  var serialNoTpms: String = "",
  var bootVersion: String = "",
  var downloadVersion: String = ""
) {
  override fun toString(): String {
    return "TpmsDeviceInfoBean(serialNo='$serialNo', serialNoTpms='$serialNoTpms', bootVersion='$bootVersion', downloadVersion='$downloadVersion')"
  }
}
