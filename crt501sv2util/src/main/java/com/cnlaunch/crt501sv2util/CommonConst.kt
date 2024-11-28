package com.cnlaunch.crt501sv2util

internal object CommonConst {

  var isDebug = true


  const val MAIN_APP_PROCESS_NAME = "com.cnlaunch.x431.crp429"
  const val MAIN_APP_GUARD_NAME = "com.cnlaunch.crpguard"
  const val MAIN_APP_PROCESS_SERVICE_NAME = "com.cnlaunch.diagnoseservice"
  const val MAIN_APP_DIAG_ACTIVITY = "com.cnlaunch.x431pro.activity.diagnose.DiagnoseActivity"
  const val MAIN_APP_FEEDBACK_ACTIVITY = "com.cnlaunch.x431pro.activity.setting.FeedbackActivity"
  const val MAIN_APP_FIRMWARE_FIX_ACTIVITY = "com.gear.diag.FirmwareFixActivitity"
  const val MAIN_APP_RECEIVER_NAME = "com.gear.crp.MainBroadCast"
  const val MAIN_APP_RECEIVER_NEW_NAME = "com.gear.crp.NewMainBroadcastReceiver"
  const val MAIN_APP_AIDL_SERVICE = "com.cnlaunch.outer.AidlForOuterServerService"
  const val BUNDLE_EXTRA_ACTION_KEY = "action"
  const val BUNDLE_EXTRA_FLAG_KEY = "flag"
  const val BUNDLE_EXTRA_DATA_KEY = "data"
  const val TPMS_REGION = "tpms_region"
  const val CUST_LANG = "cust_lang"
  const val CUST_COUNTRY = "cust_country"
  const val INIT_CONFIG = "init_config"
  const val KEY_DIAGNOSE_ID = "diagnose_id"
  const val KEY_DIAGNOSE_TYPE = "diagnose_type"
  const val KEY_OUTER_BEAN = "outerBeanString"
  const val KEY_OUTER_SPECIAL_TEXT = "outer_special_text"
  const val VALUE_TPMS_DIAG = "tpms_diag"
  const val VALUE_OBD_DIAG = "obd"
  const val VALUE_RESET_DIAG = "reset"
  const val ACTION_SEND_INIT_INFO = "com.cnlaunch.x431.init_total"
  const val ACTION_SWITCH_LANG = "com.cnlaunch.x431.init_switch_lang"
  const val ACTION_CLEAR_CACHE = "com.cnlaunch.x431.clearcache"
  const val ACTION_DEVICE_INFO = "com.cnlaunch.x431.send_device_info"
  const val ACTION_OBD_CONNECTED = "com.cnlaunch.x431.set_flag_12v"
  const val ACTION_TPMS_INIT_RESULT = "action_tpms_init_result"
  const val ACTION_SWITCH_LANG_RESULT = "action_switch_lang_result"
  const val ACTION_REFRESH_CAR_FILE = "action_refresh_car_file"
  const val KEY_INIT_RESULT = "initResult"
  const val KEY_INIT_MSG = "initMsg"
  const val KEY_SERIAL_NO = "serialNo"
  const val KEY_BOOT_VERSION = "bootVersion"
  const val KEY_DOWNLOAD_VERSION = "downloadVersion"
  const val KEY_SERIAL_NO_TPMS = "serialNo_tpms"


}