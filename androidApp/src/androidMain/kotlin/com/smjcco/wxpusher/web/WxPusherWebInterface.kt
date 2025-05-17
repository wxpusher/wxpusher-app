package com.smjcco.wxpusher.web

import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import com.huawei.hms.push.HmsMessaging
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.page.CheckActivity
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.push.huawei.HuaweiPushUtils
import com.smjcco.wxpusher.push.vivo.VIVOPushUtils
import com.smjcco.wxpusher.push.ws.WsManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.vivo.push.PushClient
import com.xiaomi.mipush.sdk.MiPushClient
import kotlinx.coroutines.launch

/**
 * web服务的接口
 */
object WxPusherWebInterface {
    private const val TAG = "WxPusherWebInterface"
    var uiModeIsNight = false
    var onWebLoadFinish: (() -> Unit?)? = null

    @JavascriptInterface
    fun showToast(toast: String?) {
        WxPusherUtils.toast(toast)
    }

    @JavascriptInterface
    fun updateDeviceInfo() {
        WxPusherLog.i(TAG, "web side要求上报token")
        DeviceApi.updateDeviceInfoAsync(null)
    }

    @JavascriptInterface
    fun getVersionName(): String {
        return WxPusherUtils.getVersionName()
    }

    @JavascriptInterface
    fun getDeviceType(): String {
        if (DeviceUtils.isMIUI()) {
            return DevicePlatform.Android_XIAOMI.getPlatform()
        } else if (PushClient.getInstance(ApplicationUtils.application).isSupport()) {
            return DevicePlatform.Android_VIVO.getPlatform()
        } else if (DeviceUtils.isHuaweiMobileServicesAvailable()) {
            return DevicePlatform.Android_HUAWEI.getPlatform()
        }
        return DevicePlatform.Android.getPlatform()
    }

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
        WxPusherLog.i(TAG, "loginSuccess() called")
        DeviceApi.updateDeviceInfoAsync(null)
    }

    /**
     * 退出登录
     */
    @JavascriptInterface
    fun logout() {
        WxPusherLog.i(TAG, "logout() called")
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

    @JavascriptInterface
    fun togoCheckPermission() {
        val intent = Intent(ApplicationUtils.application, CheckActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ApplicationUtils.application.startActivity(intent)
    }

    @JavascriptInterface
    fun getApiUrl() = WxPusherConfig.ApiUrl

}