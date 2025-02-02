package com.smjcco.wxpusher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.tencent.upgrade.core.DefaultUpgradeStrategyRequestCallback
import com.tencent.upgrade.core.UpgradeManager


class WebViewActivity : ComponentActivity() {
    companion object {
        const val INTENT_KEY_URL = "url"
    }

    private val TAG: String = "WebViewActivity"
    private var webview: WebView? = null
    private var pressBackTime = System.currentTimeMillis()
    private var preUiMode = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        //避免申请权限的时候，重复调用创建方法
        if (savedInstanceState != null) {
            return
        }
        setContentView(R.layout.web_activity)
        initWebView()
        requestPermission()
        checkUpdate()
    }

    //应用内检查升级
    private fun checkUpdate() {
        UpgradeManager.getInstance()
            .checkUpgrade(true, null, DefaultUpgradeStrategyRequestCallback())
    }

    private fun requestPermission() {
        PermissionUtils.request(
            this, Manifest.permission.POST_NOTIFICATIONS,
            "需要发送通知权限",
            "WxPusher是一个消息推送平台，当有新消息到达的时候，我们会第一时间给你发送通知，因此需要你授予发送通知的权限，否则我们无法发送消息通知，你可能会因此遗漏消息，是否授予权限？",
            "缺少通知权限",
            "本应用核心功能是发送消息通知，缺少通知权限会导致你遗漏消息。\n\n打开方式：点击“去设置”-“通知管理”-打开允许通知"
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webview = findViewById(R.id.web)
        webview?.settings?.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            // 启用DOM存储API
            domStorageEnabled = true
            // 启用数据库存储API
            databaseEnabled = true
        }

        webview?.webChromeClient = WebChromeClient()

        webview?.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e(TAG, "加载页面错误: ${error?.description}")
                super.onReceivedError(view, request, error)
            }
        }

        webview?.addJavascriptInterface(WxPusherWebInterface, "wxPusherApi")

        // 应用可能的更新
        WebBundleManager.applyUpdateIfAvailable()

        WxPusherWebInterface.onWebLoadFinish = {
            checkNightMode()
        }
        addOnNewIntentListener {
            openPageFromIntent(it)
        }

        if (!openPageFromIntent(intent)) {
            webview?.clearHistory()
            // 加载本地文件
            webview?.loadUrl("${getWebPageUrl()}#/home")
        }
    }

    private fun openPageFromIntent(intent: Intent?): Boolean {

        val url = intent?.getStringExtra(INTENT_KEY_URL)
        if (!url.isNullOrEmpty()) {
            webview?.clearHistory()
            webview?.loadUrl("${getWebPageUrl()}#/home?url=${url}")
            return true
        }
        return false
    }

    private fun getWebPageUrl(): String {
//        return "http://10.0.0.10:3000/"
        val webDir = WebBundleManager.getWebFileDir()
        return "file://${webDir.absolutePath}/index.html"
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - pressBackTime < 400) {
            //连续点击返回，直接返回页面
            finish()
            return
        }
        pressBackTime = System.currentTimeMillis()
        if (webview?.canGoBack() == true) {
            webview?.goBack()
            return
        }
        super.onBackPressed()
    }

    /**
     * 监听主题颜色的变化
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        checkNightMode()
    }

    private fun checkNightMode() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        //UI模式没变，就不调用
        if (preUiMode == currentNightMode) {
            return
        }
        preUiMode = currentNightMode
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                // 浅色模式
                onUiModeChanged(false)
            }

            Configuration.UI_MODE_NIGHT_YES -> {
                // 深色模式
                onUiModeChanged(true)
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                //其他未指定模式
                onUiModeChanged(false)
            }
        }
    }

    private fun onUiModeChanged(night: Boolean) {
        Log.d(TAG, "onUiModeChanged() called with: night = $night")
        WxPusherWebInterface.uiModeIsNight = night
        webview?.evaluateJavascript("window.onUiModeChanged && window.onUiModeChanged(${night})") {
        }
    }
}