package com.smjcco.wxpusher.utils

object DeviceUtils {
    //是否是小米设备
    fun isMIUI(): Boolean {
        return "Xiaomi".equals(android.os.Build.MANUFACTURER, true)
    }
}