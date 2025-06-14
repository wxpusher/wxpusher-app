package com.smjcco.wxpusher.base

import android.content.Context
import android.content.SharedPreferences

private lateinit var WxpSaveServiceAndroidSP: SharedPreferences

actual fun WxpSaveService_get(key: String): String? {
    return WxpSaveServiceAndroidSP.getString(key, null)
}

actual fun WxpSaveService_set(key: String, value: String?) {
    if (value.isNullOrEmpty()) {
        WxpSaveServiceAndroidSP.edit().remove(key).apply()
    } else {
        WxpSaveServiceAndroidSP.edit().putString(key, value).apply()
    }
}

actual fun WxpSaveService_init() {
    WxpSaveServiceAndroidSP =
        ApplicationUtils.application.getSharedPreferences("wxpusher-kv", Context.MODE_PRIVATE)
}


