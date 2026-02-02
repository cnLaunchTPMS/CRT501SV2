package com.cnlaunch.testSiming.bean

data class CarYear(
    val name: String
)

data class CarModel(
    val name: String,
    val years: MutableList<CarYear> = mutableListOf()
)

data class CarMake(
    val name: String,
    val models: MutableList<CarModel> = mutableListOf()
)


