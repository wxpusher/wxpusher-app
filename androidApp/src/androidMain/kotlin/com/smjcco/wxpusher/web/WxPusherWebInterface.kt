package com.smjcco.wxpusher.web

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.page.CheckActivity
import com.smjcco.wxpusher.page.WebDetailActivity
import com.smjcco.wxpusher.push.ws.WsManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.tencent.upgrade.core.UpgradeManager
import com.tencent.upgrade.core.UpgradeReqCallbackForUserManualCheck
import kotlinx.coroutines.launch
import java.net.URI

/**
 * web服务的接口
 */
class WxPusherWebInterface {
    private var webView: WebView;
    private val TAG = "WxPusherWebInterface"
    var uiModeIsNight = false
    var onWebLoadFinish: (() -> Unit?)? = null
    var activity: Activity
    var webUrl: String? = null

    companion object {
        // 白名单域名列表
        private val WHITELIST_HOSTS = setOf(
            "wxpusher.zjiecode.com",
            "static.zjiecode.com",
            "wxpusher.test.zjiecode.com",
            "10.0.0.11",
        )
    }

    constructor(activity: Activity, webView: WebView) {
        this.webView = webView
        this.activity = activity
    }

    private fun isHostAllowed(url: String?): Boolean {
        val currentUrl = url ?: return false
        return try {
            val uri = URI(currentUrl)
            //加载本地资源
            if (uri.path != null && uri.scheme == "file" && uri.path.startsWith("/data/user/0/${ApplicationUtils.application.packageName}")) {
                return true
            }
            val host = uri.host
            WHITELIST_HOSTS.contains(host)
        } catch (e: Exception) {
            WxPusherLog.w(TAG, "验证host失败: ${e.message}")
            false
        }
    }

    private fun checkSecurity(): Boolean {
        if (!isHostAllowed(webUrl)) {
            WxPusherLog.w(TAG, "非白名单域名访问接口: $webUrl")
            return false
        }
        return true
    }

    @JavascriptInterface
    fun showToast(toast: String?) {
        if (!checkSecurity()) return
        WxPusherUtils.toast(toast)
    }

    @JavascriptInterface
    fun updateDeviceInfo() {
        WxPusherLog.i(TAG, "web side要求上报token")
        DeviceApi.updateDeviceInfoAsync(DeviceUtils.getPlatform())
    }

    @JavascriptInterface
    fun getVersionName(): String {
        return WxPusherUtils.getVersionName()
    }

    @JavascriptInterface
    fun getPlatform(): String {
        return DeviceUtils.getPlatform().getPlatform()
    }

    @JavascriptInterface
    fun getDeviceName(): String {
        return Build.BRAND + " " + Build.MODEL
    }

    @JavascriptInterface
    fun getPushToken(): String? {
        if (!checkSecurity()) return null
        return AppDataUtils.getPushToken()
    }

    @JavascriptInterface
    fun getByKey(key: String): String? {
        if (!checkSecurity()) return null
        return SaveUtils.getByKey(key)
    }

    @JavascriptInterface
    fun setKeyValue(key: String, value: String) {
        if (!checkSecurity()) return
        SaveUtils.setKeyValue(key, value)
    }

    @JavascriptInterface
    fun getLoginInfo(): String? {
        if (!checkSecurity()) return null
        return AppDataUtils.getLoginInfoStr()
    }

    @JavascriptInterface
    fun saveLoginInfo(loginInfoStr: String?) {
        if (!checkSecurity()) return
        AppDataUtils.saveLoginInfo(loginInfoStr)
    }

    /**
     * 登录成功的时候调用
     */
    @JavascriptInterface
    fun loginSuccess() {
        if (!checkSecurity()) return
        WxPusherLog.i(TAG, "loginSuccess() called")
        DeviceApi.updateDeviceInfoAsync(DeviceUtils.getPlatform())
    }

    /**
     * 退出登录
     */
    @JavascriptInterface
    fun logout() {
        if (!checkSecurity()) return
        WxPusherLog.i(TAG, "logout() called")
    }

    @JavascriptInterface
    fun getWsConnectStatus(): Int {
        if (!checkSecurity()) return -1
        return WsManager.getConnectStatus().code
    }

    @JavascriptInterface
    fun uiModeIsNight(): Boolean {
        return uiModeIsNight
    }

    @JavascriptInterface
    fun loadFinish() {
        WxPusherUtils.getMainScope().launch {
            onWebLoadFinish?.invoke()
        }
    }

    @JavascriptInterface
    fun togoCheckPermission() {
        if (!checkSecurity()) return
        val intent = Intent(ApplicationUtils.application, CheckActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ApplicationUtils.application.startActivity(intent)
    }

    @JavascriptInterface
    fun getApiUrl(): String {
        return WxPusherConfig.ApiUrl
    }


    /**
     * 分享内容
     */
    @JavascriptInterface
    fun share(content: String?) {
        if (!checkSecurity()) return
        if (content.isNullOrEmpty()) {
            WxPusherLog.w(TAG, "复制失败，content=null")
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, content)
        val chooser = Intent.createChooser(intent, "分享到")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ApplicationUtils.application.startActivity(chooser)
    }

    /**
     * 在浏览器中打开这个url
     */
    @JavascriptInterface
    fun openInBrowser(url: String?) {
        if (!checkSecurity()) return
        if (url.isNullOrEmpty()) {
            WxPusherLog.w(TAG, "openInBrowser失败，url=null")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ApplicationUtils.application.startActivity(intent)
        } catch (e: Exception) {
            WxPusherLog.w(TAG, "打开浏览器失败: ${e.message}")
            WxPusherUtils.toast("打开浏览器失败")
        }
    }

    /**
     * 复制内容到剪贴板
     */
    @JavascriptInterface
    fun copy(text: String?) {
        if (!checkSecurity()) return
        if (text.isNullOrEmpty()) {
            WxPusherLog.w(TAG, "复制失败，text=null")
            return
        }
        val clipboardManager =
            ApplicationUtils.application.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = android.content.ClipData.newPlainText("WxPusher", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    /**
     * 获取浏览器当前的url
     */
    @JavascriptInterface
    fun getCurrentWebUrl(): String? {
        if (!checkSecurity()) return null
        return webView.url
    }

    @JavascriptInterface
    fun checkAppUpdate() {
        if (!checkSecurity()) return
        UpgradeManager.getInstance()
            .checkUpgrade(true, null, object : UpgradeReqCallbackForUserManualCheck() {
                override fun onReceivedNoStrategy() {
                    WxPusherUtils.toast("已经是最新版本")
                }
            })
    }

    @JavascriptInterface
    fun openUrl(url: String?) {
        if (!checkSecurity()) return
        if (url.isNullOrEmpty()) {
            return
        }
        val intent = Intent(activity, WebDetailActivity::class.java)
        intent.putExtra(WebDetailActivity.INTENT_KEY_URL, url)
        activity.startActivity(intent)
    }
}