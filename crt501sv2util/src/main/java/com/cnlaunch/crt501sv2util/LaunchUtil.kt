package com.cnlaunch.crt501sv2util

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.lang.Thread.sleep

/**
 * 元征工具类
 *
 * @author BornVon
 * @date 2024-08-11
 */
class LaunchUtil private constructor(context: Context) {
  
  private companion object{
    private const val TAG = "LaunchUtil"
    private const val MAIN_APP_PROCESS_NAME = "com.cnlaunch.x431.crp429"
    private const val MAIN_APP_PROCESS_SERVICE_NAME = "com.cnlaunch.diagnoseservice"
    private const val MAIN_APP_DIAG_ACTIVITY = "com.cnlaunch.x431pro.activity.diagnose.DiagnoseActivity"
    private const val MAIN_APP_RECEIVER_NAME = "com.gear.crp.MainBroadCast"
    private const val BUNDLE_EXTRA_ACTION_KEY = "action"
    private const val BUNDLE_EXTRA_FLAG_KEY = "flag"
    private const val BUNDLE_EXTRA_DATA_KEY = "data"
    private const val TPMS_REGION = "tpms_region"
    private const val CUST_LANG = "cust_lang"
    private const val CUST_COUNTRY = "cust_country"
    private const val INIT_CONFIG = "init_config"
    private const val KEY_DIAGNOSE_ID = "diagnose_id"
    private const val KEY_OUTER_BEAN = "outerBeanString"
    private const val VALUE_TPMS_DIAG = "tpms_diag"
    private const val VALUE_OBD_DIAG = "obd"
    private const val VALUE_RESET_DIAG = "reset"
    private const val ACTION_SEND_INIT_INFO = "com.cnlaunch.x431.init_total"
    private const val ACTION_CLEAR_CACHE = "com.cnlaunch.x431.clearcache"
    private const val ACTION_DEVICE_INFO = "com.cnlaunch.x431.send_device_info"
    private const val ACTION_OBD_CONNECTED = "com.cnlaunch.x431.set_flag_12v"
    
    
    
    private var obdCheckRunnable: ObdCheckRunnable? = null
    private var obdCheckThread: Thread? = null
    private val obdCheckLock = Any()
 

    //静态的实例变量
    @Volatile
    private var instance: LaunchUtil? = null

    // 获取 LaunchUtil 的单例实例
    @JvmStatic
    fun getInstance(context: Context): LaunchUtil {
      if (instance == null) {
        synchronized(LaunchUtil::class.java) {
          if (instance == null) {
            instance = LaunchUtil(context)
          }
        }
      }
      if (instance == null){
        throw RuntimeException(Exception("instance 为空"))
      }

      return instance!!
    }
  }


  //通用回调
  abstract class LaunchCallback {
    fun onStart() {}
    fun onEnd(data: Any?) {}
    fun onBooleanValue(isBoolean: Boolean) {}
    fun onFloatValue(value: Float) {}
  }

  private inner class ObdCheckRunnable(private val callbackInner: LaunchCallback?) : Runnable {
    @Volatile
    private var isRunning = true

    @Volatile
    private var currentIsConnected = false
    fun switchRunning(isRun: Boolean) {
      isRunning = isRun
    }

    override fun run() {
      try {
        while (isRunning) {
          val voltageString = ComUtils.getObdVoltage()
          val originVoltage = voltageString.toFloat() * 18 / 1024 + 1.7f
          val voltage = originVoltage + (originVoltage - 8) * 0.15f
          val isConnected = !TextUtils.isEmpty(voltageString) && originVoltage > 8
          synchronized(this) {
            if (currentIsConnected != isConnected) {
              if (CommonConst.isDebug) {
                Log.d(TAG, "OBD状态变化：$isConnected")
              }
              currentIsConnected = isConnected
              callbackInner?.onBooleanValue(isConnected)
              val intent = Intent(ACTION_OBD_CONNECTED)
              intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
              intent.putExtra(BUNDLE_EXTRA_DATA_KEY, isConnected)
              contextByCheck.sendBroadcast(intent)
            }
            if (isConnected) {
              callbackInner?.onFloatValue(voltage)
            }
          }
          synchronized(this) { 
            sleep(2000L)
          }
        }
      } catch (e: Exception) {
        if (CommonConst.isDebug) {
          Log.e(TAG, "Obd连接检测异常: " + e.message)
        }
      }
    }
  }

  //是否进入过元征APP模块
  @Volatile
  private var hasGotoLaunchApp = false

  //声明一个 Context 变量
  private val mContext: Context?

  //构造函数
  init {
    //使用应用级别的 Context
    mContext = context.applicationContext
  }

  

  /**
   * 切换是否为调试模式
   * @param isDebug 是否为调试模式
   */
  fun switchDebug(isDebug: Boolean) {
    CommonConst.isDebug = isDebug
  }

  /**
   * 安全性地获取内部context
   */
  private val contextByCheck: Context get() {
      if (mContext == null) {
        throw RuntimeException(Exception("context 为空"))
      }
      return mContext
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
      contextByCheck.sendBroadcast(intent)
    } catch (e: JSONException) {
      if (CommonConst.isDebug) {
        Log.e(TAG, "初始化异常")
      }
    }
    callback?.onStart()
    val initFilter = IntentFilter()
    initFilter.addAction(ACTION_DEVICE_INFO)
    contextByCheck.registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_DEVICE_INFO == intent.action) {
          contextByCheck.unregisterReceiver(this)
        }
        callback?.onEnd(intent.getStringExtra(BUNDLE_EXTRA_DATA_KEY))
      }
    }, initFilter)
  }

  
  
  /**
   * 跳转OBD学习页面
   * @param beanForOuter 跳转用数据bean
   * @throws Exception 异常
   */
  @Throws(Exception::class)
  fun gotoObdLearn(beanForOuter: DiagTpmsBeanForOuter) {
    if (CommonConst.isDebug) {
      Log.d(TAG, "跳转OBD学习")
    }
    val intent = Intent()
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_TPMS_DIAG)
    intent.putExtra(KEY_OUTER_BEAN, beanForOuter.transmitString)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    contextByCheck.startActivity(intent)
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
    contextByCheck.startActivity(intent)
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
    contextByCheck.startActivity(intent)
    hasGotoLaunchApp = true
  }

  
  
  /**
   * 开始OBD连接监听
   */
  fun startListenObdState(launchCallback: LaunchCallback?) {
    Log.d(TAG, "开始监听OBD")
    synchronized(obdCheckLock) {
      if (obdCheckRunnable == null) {
        obdCheckRunnable = ObdCheckRunnable(launchCallback)
      }
      if (obdCheckThread == null) {
        obdCheckThread = Thread(obdCheckRunnable)
        obdCheckThread!!.start()
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
    synchronized(obdCheckLock) {
      if (obdCheckRunnable != null) {
        obdCheckRunnable!!.switchRunning(false)
        obdCheckRunnable = null
      }
      if (obdCheckThread != null) {
        obdCheckThread!!.interrupt()
        obdCheckThread = null
      }
    }
  }

  
  
  /**
   * 清理缓存
   */
  fun clearCache() {
    val intent = Intent(ACTION_CLEAR_CACHE)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
    contextByCheck.sendBroadcast(intent)
  }

  
  
  /**
   * 释放元征相关app
   */
  fun releaseLaunchApp() {
    if (hasGotoLaunchApp) {
      ComUtils.killProcess(MAIN_APP_PROCESS_NAME)
      ComUtils.killProcess(MAIN_APP_PROCESS_SERVICE_NAME)
      hasGotoLaunchApp = false
    }
  }

 
}