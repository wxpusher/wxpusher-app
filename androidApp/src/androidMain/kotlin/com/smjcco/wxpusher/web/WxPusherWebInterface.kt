package com.smjcco.wxpusher.web

import android.os.Build
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.utils.WxPusherUtils

/**
 * web服务的接口
 */
object WxPusherWebInterface {
    @JavascriptInterface
    fun showToast(toast: String?) {
        WxPusherUtils.toast(toast)
    }

    @JavascriptInterface
    fun getVersionName(): String {
        return ""
    }

    @JavascriptInterface
    fun getDeviceType() = "Android"

    @JavascriptInterface
    fun getDeviceName() = Build.BRAND + " " + Build.MODEL

    @JavascriptInterface
    fun getPushToken() = ""


    @JavascriptInterface
    fun getByKey(key: String): String? = SaveUtils.getByKey(key)

    @JavascriptInterface
    fun setKeyValue(key: String, value: String) {
        SaveUtils.setKeyValue(key, value)
    }
}