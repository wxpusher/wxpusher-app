package com.smjcco.wxpusher.web

import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.ws.WsManager
import kotlinx.coroutines.launch

/**
 * web服务的接口
 */
object WxPusherWebInterface {
    private const val TAG="WxPusherWebInterface"
    var uiModeIsNight = false
    var onWebLoadFinish: (() -> Unit?)? = null

    @JavascriptInterface
    fun showToast(toast: String?) {
        WxPusherUtils.toast(toast)
    }

    @JavascriptInterface
    fun updateDeviceInfo() {
        DeviceApi.updateDeviceInfoAsync()
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

    /**
     * 登录成功的时候调用
     */
    @JavascriptInterface
    fun loginSuccess() {
        Log.d(TAG, "loginSuccess() called")
//        NotificationManager.initSubscribeChannel()
    }

    /**
     * 退出登录
     */
    @JavascriptInterface
    fun logout() {
        Log.d(TAG, "logout() called")
        WsManager.disconnect()
    }

    @JavascriptInterface
    fun getWsConnectStatus() = WsManager.getConnectStatus().code

    @JavascriptInterface
    fun uiModeIsNight() = uiModeIsNight

    @JavascriptInterface
    fun loadFinish() {
        WxPusherUtils.getMainScope().launch {
            onWebLoadFinish?.invoke()
        }
    }
}