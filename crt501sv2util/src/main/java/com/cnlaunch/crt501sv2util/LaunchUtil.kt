package com.cnlaunch.crt501sv2util

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import android.util.Log
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_CLEAR_CACHE
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_OBD_CONNECTED
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_SEND_INIT_INFO
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_TPMS_INIT_RESULT
import com.cnlaunch.crt501sv2util.CommonConst.BUNDLE_EXTRA_DATA_KEY
import com.cnlaunch.crt501sv2util.CommonConst.CUST_COUNTRY
import com.cnlaunch.crt501sv2util.CommonConst.CUST_LANG
import com.cnlaunch.crt501sv2util.CommonConst.INIT_CONFIG
import com.cnlaunch.crt501sv2util.CommonConst.KEY_BOOT_VERSION
import com.cnlaunch.crt501sv2util.CommonConst.KEY_DIAGNOSE_ID
import com.cnlaunch.crt501sv2util.CommonConst.KEY_DOWNLOAD_VERSION
import com.cnlaunch.crt501sv2util.CommonConst.KEY_INIT_MSG
import com.cnlaunch.crt501sv2util.CommonConst.KEY_INIT_RESULT
import com.cnlaunch.crt501sv2util.CommonConst.KEY_OUTER_BEAN
import com.cnlaunch.crt501sv2util.CommonConst.KEY_OUTER_SPECIAL_TEXT
import com.cnlaunch.crt501sv2util.CommonConst.KEY_SERIAL_NO
import com.cnlaunch.crt501sv2util.CommonConst.KEY_SERIAL_NO_TPMS
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_DIAG_ACTIVITY
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_PROCESS_NAME
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_PROCESS_SERVICE_NAME
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_RECEIVER_NAME
import com.cnlaunch.crt501sv2util.CommonConst.TPMS_REGION
import com.cnlaunch.crt501sv2util.CommonConst.VALUE_OBD_DIAG
import com.cnlaunch.crt501sv2util.CommonConst.VALUE_RESET_DIAG
import com.cnlaunch.crt501sv2util.CommonConst.VALUE_TPMS_DIAG
import com.cnlaunch.crt501sv2util.bean.DiagTpmsBeanForOuter
import com.cnlaunch.crt501sv2util.bean.LanguageEnum
import com.cnlaunch.crt501sv2util.bean.TpmsDeviceInfoBean
import com.cnlaunch.crt501sv2util.bean.TpmsInitBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * 元征工具类
 *
 * @author BornVon
 * @date 2024-08-11
 */
class LaunchUtil constructor(context: Context) {
  
  companion object{
    private const val TAG = "LaunchUtil"


    //静态的实例变量
    @Volatile
    private var instance: LaunchUtil? = null

    // 获取 LaunchUtil 的单例实例
    @JvmStatic
    fun getInstance(context: Context): LaunchUtil {
      return instance ?: synchronized(this) {
        instance ?: LaunchUtil(context).also { instance = it }
      }
    }
  }



  //线程不变的CoroutineScope
  private val scopeInner by lazy {
    CoroutineScope(newSingleThreadContext(LaunchUtil::class.java.simpleName))
  }

  private var obdJob: Job? = null

  //通用回调
  open class LaunchCallback {
    open fun onStart() {}
    open fun onEnd(data: Any?) {}
    open fun onBooleanValue(isBoolean: Boolean) {}
    open fun onFloatValue(value: Float) {}
  }


  //是否进入过元征APP模块
  @Volatile
  private var hasGotoLaunchApp = false


  //声明一个 Context 变量
  private val mContext: Context by lazy {
    WeakReference<Context>(context.applicationContext).get()?:run {
      throw IllegalStateException("Context is no longer available.")
    }
  }


  

  /**
   * 切换是否为调试模式
   * @param isDebug 是否为调试模式
   */
  fun switchDebug(isDebug: Boolean) {
    CommonConst.isDebug = isDebug
  }

  
  
  /**
   * 注册与初始化
   * @param languageEnum 语言类型枚举
   * @param callback     回调，可选
   */
  fun registerAndInit(languageEnum: LanguageEnum, callback: LaunchCallback?) {
    if (CommonConst.isDebug) {
      Log.d(TAG, "执行初始化")
    }
    try {
      val intent = Intent(ACTION_SEND_INIT_INFO)
      intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
      val initInfo = JSONObject()
      initInfo.put(CUST_COUNTRY, "325")
      initInfo.put(CUST_LANG, languageEnum.index)
      initInfo.put(INIT_CONFIG, false)
      initInfo.put(TPMS_REGION, languageEnum.regionIndex)
      intent.putExtra(BUNDLE_EXTRA_DATA_KEY, initInfo.toString())
      mContext.sendBroadcast(intent)
    } catch (e: JSONException) {
      if (CommonConst.isDebug) {
        Log.e(TAG, "初始化异常:" + e.message)
      }
    }
    callback?.onStart()
    val initFilter = IntentFilter()
    initFilter.addAction(ACTION_TPMS_INIT_RESULT)
    mContext.registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_TPMS_INIT_RESULT == intent.action) {
          mContext.unregisterReceiver(this)
        }


        intent.getStringExtra(BUNDLE_EXTRA_DATA_KEY)?.let {
          val json = JSONObject(it)
          val tpmsInitBean = TpmsInitBean(
            json.getString(KEY_INIT_RESULT).toBoolean(),
            json.getString(KEY_INIT_MSG),
            TpmsDeviceInfoBean(
              json.getString(KEY_SERIAL_NO),
              json.getString(KEY_SERIAL_NO_TPMS),
              json.getString(KEY_BOOT_VERSION),
              json.getString(KEY_DOWNLOAD_VERSION),
            )
          )


          callback?.onEnd(tpmsInitBean)
        } ?: kotlin.run {
          throw RuntimeException(Exception("数据回调为空"))
        }

      }
    }, initFilter)
  }


  /**
   * 跳转OBD学习页面
   * @param beanForOuter 跳转用数据bean
   * @throws Exception 异常
   */
  @Throws(Exception::class)
  fun gotoMixFunction(
    beanForOuter: DiagTpmsBeanForOuter
  ) {
    if (CommonConst.isDebug) {
      Log.d(TAG, "跳转TPMS综合功能")
    }
    val intent = Intent()
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_TPMS_DIAG)
    intent.putExtra(KEY_OUTER_BEAN, beanForOuter.transmitString)
    intent.putExtra(KEY_OUTER_SPECIAL_TEXT,beanForOuter.specialDescFormOuter)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    mContext.startActivity(intent)
    hasGotoLaunchApp = true
  }

  
  
  /**
   * 跳转到OBD诊断
   */
  fun gotoObdDiagnose() {
    if (CommonConst.isDebug) {
      Log.d(TAG, "跳转OBD诊断")
    }
    val intent = Intent()
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_OBD_DIAG)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    mContext.startActivity(intent)
    hasGotoLaunchApp = true
  }

  
  
  /**
   * 跳转到特殊功能
   */
  fun gotoResetDiagnose() {
    if (CommonConst.isDebug) {
      Log.d(TAG, "跳转特殊功能")
    }
    val intent = Intent()
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_RESET_DIAG)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    mContext.startActivity(intent)
    hasGotoLaunchApp = true
  }

  
  
  /**
   * 开始OBD连接监听
   */
  fun startListenObdState(launchCallback: LaunchCallback?) {
    Log.d(TAG, "开始监听OBD")
    if (obdJob == null){
      obdJob = scopeInner.launch(Dispatchers.IO) {
        var currentIsConnected = false
        while (true) {
          val voltageString = ComUtils.getObdVoltage()
          val originVoltage = voltageString.toFloat() * 18 / 1024 + 1.7f
          val voltage = originVoltage + (originVoltage - 8) * 0.15f
          val isConnected = !TextUtils.isEmpty(voltageString) && originVoltage > 8

          if (isConnected) {
            launchCallback?.onFloatValue(voltage)
          }

          if (currentIsConnected != isConnected) {
            if (CommonConst.isDebug) {
              Log.d(TAG, "OBD状态变化：$isConnected")
            }
            currentIsConnected = isConnected
            scopeInner.launch(Dispatchers.Main) {
              launchCallback?.onBooleanValue(isConnected)
              val intent = Intent(ACTION_OBD_CONNECTED)
              intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
              intent.putExtra(BUNDLE_EXTRA_DATA_KEY, isConnected)
              mContext.sendBroadcast(intent)
            }
          }
          delay(2000L)
        }
      }
    }

    if (obdJob?.isActive == true) {
      scopeInner.launch {
        obdJob?.start()
      }
    }
  }




  
  
  /**
   * 停止OBD连接监听
   */
  fun stopListenObdState() {
    if (CommonConst.isDebug) {
      Log.d(TAG, "停止监听OBD")
    }

    obdJob?.cancel()
    obdJob = null
  }

  
  
  /**
   * 清理缓存
   */
  fun clearCache() {
    if (CommonConst.isDebug) {
      Log.d(TAG, "清理缓存")
    }
    val intent = Intent(ACTION_CLEAR_CACHE)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
    mContext.sendBroadcast(intent)
  }


  /**
   * 跳转到工厂测试
   */
  fun gotoLaunchFactoryTest(){
    if (CommonConst.isDebug) {
      Log.d(TAG, "跳转工厂测试")
    }
    ComUtils.gotoFactory()
  }


  /**
   * 释放元征相关app
   */
  fun releaseLaunchApp() {
    if (CommonConst.isDebug) {
      Log.d(TAG, "清理缓存")
    }
    if (hasGotoLaunchApp) {
      ComUtils.killProcess(MAIN_APP_PROCESS_NAME)
      ComUtils.killProcess(MAIN_APP_PROCESS_SERVICE_NAME)
      hasGotoLaunchApp = false
    }
  }

 
}