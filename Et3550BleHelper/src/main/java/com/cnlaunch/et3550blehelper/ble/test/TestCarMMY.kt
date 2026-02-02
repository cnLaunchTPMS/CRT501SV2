package com.cnlaunch.et3550blehelper.ble.test

import android.util.Log
import com.cnlaunch.et3550blehelper.ble.api.EnumEt3550UUID
import com.cnlaunch.et3550blehelper.ble.api.Et3550BleApiProvider
import com.cnlaunch.et3550blehelper.ble.api.Et3550BleViewModel
import com.cnlaunch.testSiming.bean.CarMake
import com.cnlaunch.testSiming.bean.CarModel
import com.cnlaunch.testSiming.bean.CarYear
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

object TestCarMMY {


  suspend fun loadAllCars(bleApiProvider: Et3550BleApiProvider): List<CarMake> {
    val result = mutableListOf<CarMake>()



    // 1. 请求品牌
    write(bleApiProvider, EnumEt3550UUID.UUID_MAKE_LIST)

    delay(100)
    val makeJson = read(bleApiProvider, EnumEt3550UUID.UUID_MAKE_LIST)
    val makeResp = Gson().fromJson(makeJson, DrnResponse::class.java)

    Log.e("xxxxxxxxx11111", makeJson.toString())

    makeResp.LSMK.forEach { makeMap ->

      makeMap.keys.forEach { makeIdSingle ->

        val makeName = makeMap[makeIdSingle]!!
        val make = CarMake(makeName)
        result.add(make)

        // 2. 选择品牌
        write(bleApiProvider, EnumEt3550UUID.UUID_MAKE_LIST, """{"MMK":$makeIdSingle}""")
        delay(100)
        val modelJson = read(bleApiProvider, EnumEt3550UUID.UUID_MODEL_LIST)
        val modelResp = Gson().fromJson(modelJson, ModelResponse::class.java)

        Log.e("xxxxxxxxx222222", modelJson.toString())


        modelResp.LSMD.forEach { modelMap ->
          modelMap.keys.forEach { modelIdSingle ->
            val modelName = modelMap[modelIdSingle]!!
            val model = CarModel(modelName)
            make.models.add(model)

            // 3. 选择车型
            write(bleApiProvider, EnumEt3550UUID.UUID_MODEL_LIST, """{"MMD":$modelIdSingle}""")
            delay(100)
            val yearJson = read(bleApiProvider, EnumEt3550UUID.UUID_YEAR_LIST)
            val yearResp = Gson().fromJson(yearJson, YearResponse::class.java)
            Log.e("xxxxxxxxx3333333", yearJson)
            yearResp.LSYR.forEach { yearMap ->
              yearMap.values.forEach {
                model.years.add(CarYear(it))
              }

            }
          }

        }
      }

    }

    return result
  }


  suspend fun write(
    bleApiProvider: Et3550BleApiProvider,
    uuid: EnumEt3550UUID,
    json: String? = null
  ) =
    suspendCancellableCoroutine<Boolean> { cont ->
      bleApiProvider.writeData(uuid, json ?: "",
        object : Et3550BleViewModel.Et3550BleWriteCallback {
          override fun onWriteResult(b: Boolean, s: String) {
            cont.resume(b) {}
          }

          override fun onLoadingStatusChanged(b: Boolean) {}
        })
    }

  suspend fun read(
    bleApiProvider: Et3550BleApiProvider,
    uuid: EnumEt3550UUID
  ): String = suspendCancellableCoroutine { cont ->

    var lastJson = ""

    bleApiProvider.readData(uuid,
      object : Et3550BleViewModel.Et3550BleReadCallback {

        override fun onReadResult(b: Boolean, s: String, s1: String) {
          if (b) {
            // 始终保存最新快照
            lastJson = s
          }
        }

        override fun onLoadingStatusChanged(loading: Boolean) {
          // 关键点：loading=false 才是真正结束
          if (!loading && lastJson.isNotEmpty()) {
            cont.resume(lastJson) {}
          }
        }
      })
  }





}