package com.smjcco.wxpusher.base

import android.content.Context
import android.content.SharedPreferences

private lateinit var WxpSaveServiceAndroidSP: SharedPreferences

actual fun ExpWxpSaveService_get(key: String): String? {
    return WxpSaveServiceAndroidSP.getString(key, null)
}

actual fun ExpWxpSaveService_set(key: String, value: String?) {
    if (value.isNullOrEmpty()) {
        WxpSaveServiceAndroidSP.edit().remove(key).apply()
    } else {
        WxpSaveServiceAndroidSP.edit().putString(key, value).apply()
    }
}

actual fun ExpWxpSaveService_getDouble(key: String): Double? {
    return ExpWxpSaveService_get(key)?.toDouble()
}

actual fun ExpWxpSaveService_setDouble(key: String, value: Double) {
    ExpWxpSaveService_set(key, value.toString())
}


actual fun ExpWxpSaveService_init() {
    WxpSaveServiceAndroidSP =
        ApplicationUtils.application.getSharedPreferences("wxpusher-kv", Context.MODE_PRIVATE)
}

actual fun ExpWxpSaveService_remove(key: String) {
    WxpSaveServiceAndroidSP.edit().remove(key).apply()
}


