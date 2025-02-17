package com.cnlaunch.crt501sv2util

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import com.cnlaunch.crt501sv2util.ComUtils.getTpmsPointState
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_CLEAR_CACHE
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_OBD_CONNECTED
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_REFRESH_CAR_FILE
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_SEND_INIT_INFO
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_SWITCH_LANG
import com.cnlaunch.crt501sv2util.CommonConst.ACTION_SWITCH_LANG_RESULT
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
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_AIDL_SERVICE
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_DIAG_ACTIVITY
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_FACTORY_ACTIVITY
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_FEEDBACK_ACTIVITY
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_FIRMWARE_FIX_ACTIVITY
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_GUARD_NAME
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_PROCESS_NAME
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_PROCESS_SERVICE_NAME
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_RECEIVER_NAME
import com.cnlaunch.crt501sv2util.CommonConst.MAIN_APP_RECEIVER_NEW_NAME
import com.cnlaunch.crt501sv2util.CommonConst.TPMS_REGION
import com.cnlaunch.crt501sv2util.CommonConst.VALUE_OBD_DIAG
import com.cnlaunch.crt501sv2util.CommonConst.VALUE_RESET_DIAG
import com.cnlaunch.crt501sv2util.CommonConst.VALUE_TPMS_DIAG
import com.cnlaunch.crt501sv2util.bean.DiagTpmsBeanForOuter
import com.cnlaunch.crt501sv2util.bean.LanguageEnum
import com.cnlaunch.crt501sv2util.bean.TpmsDeviceInfoBean
import com.cnlaunch.crt501sv2util.bean.TpmsInitBean
import com.cnlaunch.outer.IAidlForOuter
import com.cnlaunch.outer.IAidlForOuterCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

  companion object {
    private const val TAG = "LaunchUtil"

    private const val SOUND_REPEAT_TIMES_2 = 2
    private const val SOUND_REPEAT_TIMES_8 = 8



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
    open fun onJsonValue(jsonObject: JSONObject) {}
    open fun onBooleanFunValue(functionOthers: (Boolean) -> Unit) {}
    open fun onError(value: String) {}

  }


  //是否进入过元征APP模块
  @Volatile
  private var hasGotoLaunchApp = false


  //声明一个 Context 变量
  private val mContext: Context by lazy {
    WeakReference<Context>(context.applicationContext).get() ?: run {
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

    logInner("执行初始化")

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
      logInner("初始化异常:" + e.message)
      callback?.onError(e.message ?: "")
    }
    callback?.onStart()
    val initFilter = IntentFilter()
    initFilter.addAction(ACTION_TPMS_INIT_RESULT)
    mContext.registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_TPMS_INIT_RESULT == intent.action) {
          mContext.unregisterReceiver(this)
        }


        val tpmsInitBean = TpmsInitBean(
          intent.getBooleanExtra(KEY_INIT_RESULT, false),
          intent.getStringExtra(KEY_INIT_MSG) ?: "",
          TpmsDeviceInfoBean()
        )

        intent.getStringExtra(BUNDLE_EXTRA_DATA_KEY)?.let {
          val json = JSONObject(it)
          tpmsInitBean.deviceInfoBean = TpmsDeviceInfoBean(
            json.getString(KEY_SERIAL_NO),
            json.getString(KEY_SERIAL_NO_TPMS),
            json.getString(KEY_BOOT_VERSION),
            json.getString(KEY_DOWNLOAD_VERSION),
          )
          callback?.onEnd(tpmsInitBean)
        } ?: kotlin.run {
          tpmsInitBean.result = false
          callback?.onEnd(tpmsInitBean)
        }

      }
    }, initFilter)
  }


  /**
   * 切换语言
   * @param languageEnum 语言类型枚举
   * @param callback     回调，可选
   */
  fun switchLang(languageEnum: LanguageEnum, callback: LaunchCallback?) {

    logInner("执行切换语言")

    try {
      val intent = Intent(ACTION_SWITCH_LANG)
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
        Log.e(TAG, "切换语言异常:" + e.message)
        callback?.onError(e.message ?: "")
      }
    }
    callback?.onStart()
    val initFilter = IntentFilter()
    initFilter.addAction(ACTION_SWITCH_LANG_RESULT)
    mContext.registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_SWITCH_LANG_RESULT == intent.action) {
          mContext.unregisterReceiver(this)
        }
        callback?.onEnd(intent.getBooleanExtra(KEY_INIT_RESULT, false))
      }
    }, initFilter)
  }


  /**
   * 跳转TPMS综合功能
   * @param beanForOuter 跳转用数据bean
   * @throws Exception 异常
   */
  @Throws(Exception::class)
  fun gotoMixFunction(
    beanForOuter: DiagTpmsBeanForOuter,
  ) {

    logInner("跳转TPMS综合功能")

    //刷新文件
    refreshFunctionFile()


    val intent = Intent()
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_TPMS_DIAG)
    intent.putExtra(KEY_OUTER_BEAN, beanForOuter.transmitString)
    intent.putExtra(KEY_OUTER_SPECIAL_TEXT, beanForOuter.specialDescFormOuter)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    mContext.startActivity(intent)
    hasGotoLaunchApp = true

  }


  /**
   * 跳转OBD学习（带回调）页面
   * @param beanForOuter 跳转用数据bean
   * @param highFrequencyCallback 高频学习回调
   * @throws Exception 异常
   */
  fun gotoObdLearn(
    beanForOuter: DiagTpmsBeanForOuter,
    highFrequencyCallback: LaunchCallback?
  ) {

    logInner("跳转OBD学习")

    //刷新文件
    refreshFunctionFile()


    val intent = Intent()
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_TPMS_DIAG)
    beanForOuter.functionEnum = DiagTpmsBeanForOuter.TpmsFunctionEnum.OBD_LEARN
    intent.putExtra(KEY_OUTER_BEAN, beanForOuter.transmitString)
    intent.putExtra(KEY_OUTER_SPECIAL_TEXT, beanForOuter.specialDescFormOuter)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)

    //绑定AIDL服务
    mContext.bindService(
      Intent().setComponent(ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_AIDL_SERVICE)),
      object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
          val aidlService = IAidlForOuter.Stub.asInterface(iBinder)
          aidlService.registerCallback(object : IAidlForOuterCallback.Stub() {


            override fun getTag(): String {
              return "client"
            }

            override fun onOBDLearnData(tireCount: Int, tireIndex: Int) {

              if (tireCount < 0) {
                highFrequencyCallback?.onError("-1")
              }

              highFrequencyCallback?.onJsonValue(JSONObject().apply {
                put("tireCount", tireCount)
                put("tireIndex", tireIndex)
              })
            }

            override fun onOBDLearnResult(isSuccess: Boolean) {}
          })


          //给外部暴露回调代理
          highFrequencyCallback?.onBooleanFunValue {
            aidlService.sendOBDLearnResult(it)
          }

          mContext.startActivity(intent)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
          highFrequencyCallback?.onError("服务断连")
        }
      },
      Context.BIND_AUTO_CREATE
    )


    hasGotoLaunchApp = true
  }


  /**
   * 跳转到OBD诊断
   */
  fun gotoObdDiagnose() {

    logInner("跳转OBD诊断")


    //刷新文件
    refreshFunctionFile()


    val intent = Intent()
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_OBD_DIAG)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    mContext.startActivity(intent)
    hasGotoLaunchApp = true
  }


  /**
   * 跳转到特殊功能
   */
  fun gotoResetDiagnose() {
    logInner("跳转特殊功能")

    //刷新文件
    refreshFunctionFile()


    val intent = Intent()
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(KEY_DIAGNOSE_ID, VALUE_RESET_DIAG)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_DIAG_ACTIVITY)
    mContext.startActivity(intent)
    hasGotoLaunchApp = true
  }


  /**
   * 跳转诊断反馈
   */
  fun gotoFeedBack() {
    logInner("跳转元征诊断反馈")
    mContext.startActivity(
      Intent().apply {
        component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_FEEDBACK_ACTIVITY)
      }
    )

    hasGotoLaunchApp = true
  }


  fun gotoFirmwareFix() {

    logInner("跳转元征固件修复")

    //刷新文件
    refreshFunctionFile()


    val intent = Intent().apply {
      component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_FIRMWARE_FIX_ACTIVITY)
    }
    mContext.startActivity(intent)
    hasGotoLaunchApp = true
  }


  /**
   * 开始OBD连接监听
   */
  fun startListenObdState(launchCallback: LaunchCallback?) {
    logInner("开始监听OBD")


    // 请求取消
    obdJob?.cancel()

    // 确保旧任务完成
    obdJob?.let { runBlocking { it.join() } }

    obdJob = scopeInner.launch(Dispatchers.IO) {
      var currentIsConnected = false
      while (isActive) {

        // 确保协程可被取消
        ensureActive()

        val voltageString = ComUtils.getObdVoltage()
        var isConnected: Boolean = false

        val originVoltage = voltageString.toFloatOrNull()?.let { it * 18 / 1024 + 1.7f } ?: continue
        val voltage = originVoltage + (originVoltage - 8) * 0.15f
        isConnected = originVoltage > 8
        launchCallback?.onFloatValue(voltage)

        if (currentIsConnected != isConnected) {
          logInner("OBD状态变化：$isConnected")
          currentIsConnected = isConnected
          playSound(if (isConnected) SOUND_REPEAT_TIMES_8 else SOUND_REPEAT_TIMES_2)
          withContext(Dispatchers.Main) {
            launchCallback?.onBooleanValue(isConnected)
            mContext.sendBroadcast(
              Intent(ACTION_OBD_CONNECTED).apply {
                component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
                putExtra(BUNDLE_EXTRA_DATA_KEY, isConnected)
              }
            )
          }
        }

        delay(1000L)
      }
    }
  }



  /**
   * 停止OBD连接监听
   */
  fun stopListenObdState() {
    logInner("停止监听OBD")
    obdJob?.cancel()
    obdJob?.let { runBlocking { it.join() } }
    obdJob = null
  }



  /**
   * 开关USB供电
   * @param callBack 回调
   */
  fun switchUSBPower(
    isOpen: Boolean,
    callBack: (Boolean) -> Unit = {}
  ) {
    logInner("执行USB供电 ：$isOpen")

    scopeInner.launch {
      ComUtils.powerUSB(isOpen)
      delay(500)
      callBack.invoke(getTpmsPointState() == isOpen)
    }
  }


  /**
   * 清理缓存
   */
  fun clearCache() {
    logInner("清理缓存")
    val intent = Intent(ACTION_CLEAR_CACHE)
    intent.component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NAME)
    mContext.sendBroadcast(intent)
  }


  /**
   * 跳转到工厂测试
   */
  fun gotoLaunchFactoryTest() {
    logInner("跳转工厂测试")
    val intent = Intent().apply {
      component = ComponentName(MAIN_APP_GUARD_NAME, MAIN_APP_FACTORY_ACTIVITY)
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    mContext.startActivity(intent)
    scopeInner.launch(Dispatchers.Main) {
      delay(800)
      mContext.startActivity(intent)
    }

  }


  /**
   * 刷新功能目录
   */
  fun refreshFunctionFile() {
    logInner("刷新功能文件")
    mContext.sendBroadcast(Intent(ACTION_REFRESH_CAR_FILE).apply {
      component = ComponentName(MAIN_APP_PROCESS_NAME, MAIN_APP_RECEIVER_NEW_NAME)
    })

  }


  /**
   * 释放元征相关app
   */
  fun releaseLaunchApp() {
    logInner("释放元征APP")
    if (hasGotoLaunchApp) {
      ComUtils.killProcess(MAIN_APP_PROCESS_NAME)
      ComUtils.killProcess(MAIN_APP_GUARD_NAME)
      ComUtils.killProcess(MAIN_APP_PROCESS_SERVICE_NAME)
      hasGotoLaunchApp = false
      ComUtils.powerOBD(true)
    }
  }



  fun releaseSiming() {
    logInner("释放思明")
    if (hasGotoLaunchApp) {
      ComUtils.killProcess("com.custompad.tpms")

      hasGotoLaunchApp = false
    }
  }


  /**
   * 调用蜂鸣器
   * @param repeatTimes 次数
   */
  private fun playSound(repeatTimes : Int){
    logInner("调用蜂鸣器")
    ComUtils.playSound(repeatTimes)
  }


  private fun logInner(msg: String) {
    if (CommonConst.isDebug) {
      Log.d(TAG, msg)
    }
  }


}