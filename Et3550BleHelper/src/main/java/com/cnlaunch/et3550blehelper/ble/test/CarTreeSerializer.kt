package com.cnlaunch.et3550blehelper.ble.test

import com.cnlaunch.testSiming.bean.CarMake
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object CarTreeSerializer {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(list: List<CarMake>): String {
        return gson.toJson(list)
    }

    fun fromJson(json: String): List<CarMake> {
        val type = object : TypeToken<List<CarMake>>() {}.type
        return gson.fromJson(json, type)
    }
}
