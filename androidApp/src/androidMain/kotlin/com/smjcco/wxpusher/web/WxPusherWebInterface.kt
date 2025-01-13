package com.smjcco.wxpusher.web

import android.os.Build
import android.webkit.JavascriptInterface
import com.smjcco.wxpusher.utils.AppDataUtils
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
        return WxPusherUtils.getVersionName()
    }

    @JavascriptInterface
    fun getDeviceType() = "Android"

    @JavascriptInterface
    fun getDeviceName() = Build.BRAND + " " + Build.MODEL

    @JavascriptInterface
    fun getPushToken() = AppDataUtils.getPushToken()

    @JavascriptInterface
    fun getByKey(key: String): String? = SaveUtils.getByKey(key)

    @JavascriptInterface
    fun setKeyValue(key: String, value: String) {
        SaveUtils.setKeyValue(key, value)
    }

    @JavascriptInterface
    fun getLoginInfo(): String? = AppDataUtils.getLoginInfoStr()

    @JavascriptInterface
    fun saveLoginInfo(loginInfoStr: String?) {
        AppDataUtils.saveLoginInfo(loginInfoStr)
    }
}