package com.smjcco.wxpusher.page

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.tencent.upgrade.core.DefaultUpgradeStrategyRequestCallback
import com.tencent.upgrade.core.UpgradeManager
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageHelper


class WebViewActivity : ComponentActivity() {
    companion object {
        const val INTENT_KEY_URL = "url"
    }

    private val TAG: String = "WebViewActivity"
    private var webview: WebView? = null
    private var webContainer: View? = null
    private var pressBackTime = System.currentTimeMillis()
    private var preUiMode = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        //避免申请权限的时候，重复调用创建方法
        if (savedInstanceState != null) {
            return
        }
        if (SaveUtils.getByKey(getString(R.string.privacy_key)) != "1") {
            startMainActivity()
            return
        }
        setContentView(R.layout.web_activity)
        webContainer = findViewById(R.id.web_container)
        enableEdgeToEdge()
        initWebView()
        requestPermission()
        checkUpdate()
//        noteKeepAlive()
    }

    /**
     * 启动隐私
     */
    private fun startMainActivity() {
        val intent = Intent(this, AgreePrivateActivity::class.java)
        startActivity(intent)
        finish()
    }

    //应用内检查升级
    private fun checkUpdate() {
        UpgradeManager.getInstance()
            .checkUpgrade(true, null, DefaultUpgradeStrategyRequestCallback())
    }

    private fun requestPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "android.permission.POST_NOTIFICATIONS"
        PermissionRequester(
            this, permission,
            "需要发送通知权限",
            "WxPusher是一个消息推送平台，当有新消息到达的时候，我们会第一时间给你发送通知，因此需要你授予发送通知的权限，否则我们无法发送消息通知，你可能会因此遗漏消息，是否授予权限？",
            "缺少通知权限",
            "本应用核心功能是发送消息通知，缺少通知权限会导致你遗漏消息。\n\n打开方式：点击“去设置”-“通知管理”-打开允许通知"
        ) {

            PermissionUtils.gotoNotificationSettingPage()
        }.request {

        }
    }

    /**
     * 提示保活
     */
    private fun noteKeepAlive() {
        if (SaveUtils.getByKey("noteKeepAlive") == "1") {
            return
        }
        AlertDialog.Builder(this)
            .setTitle("保活提示")
            .setMessage("由于Android的系统限制，应用在后台会被限制允许，导致收不到消息 ，请打开后台限制。")
            .setPositiveButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                val intent = Intent(ApplicationUtils.application, CheckActivity::class.java)
                startActivity(intent)
            }
            .setCancelable(false)
            .setNegativeButton("不再提醒") { dialog, _ ->
                dialog?.dismiss()
                SaveUtils.setKeyValue("noteKeepAlive", "1")
            }
            .create().show()
    }

    override fun onResume() {
        super.onResume()
        showSettingGuide()
    }

    /**
     * 提示保活
     */
    private fun showSettingGuide() {
        //针对小米，还没有创建推送通道 ，就不进行提醒
        if (!NotificationManager.hasNotificationChannel("mipush|com.smjcco.wxpusher|135072")) {
            return
        }
        if (SaveUtils.getByKey("alertTips") == "1") {
            return
        }
        AlertDialog.Builder(this)
            .setTitle("请打开锁屏提醒")
            .setMessage("为了避免锁屏遗漏通知，请点击「去设置」，选择「订阅消息」-「在锁定屏幕上」设置为【显示通知】\n\n在设置里，你还可以自定义提示铃声。")
            .setPositiveButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                PermissionUtils.gotoNotificationSettingPage()
            }
            .setCancelable(false)
            .setNegativeButton("不再提醒") { dialog, _ ->
                dialog?.dismiss()
                SaveUtils.setKeyValue("alertTips", "1")
            }
            .create().show()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webview = findViewById(R.id.web)

        WebView.setWebContentsDebuggingEnabled(true)
        webview?.settings?.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            // 启用DOM存储API
            domStorageEnabled = true
            // 启用数据库存储API
            databaseEnabled = true
            //在https里面允许加载http的内容
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
//            webview?.loadUrl("https://wxpusher.zjiecode.com/admin/agreement/index-argeement.html#qqq")
        }
    }

    private fun openPageFromIntent(intent: Intent?): Boolean {
        //ws 和华为，直接读取url判断打开页面
        val url = intent?.getStringExtra(INTENT_KEY_URL)
        if (!url.isNullOrEmpty()) {
            webview?.clearHistory()
            webview?.loadUrl("${getWebPageUrl()}#/home?url=${url}")
            return true
        }
        //小米推送的消息，如果有 url，直接打开地址
        val miPushMessage =
            intent?.getSerializableExtra(PushMessageHelper.KEY_MESSAGE) as MiPushMessage?
        val miPushUrl = miPushMessage?.extra?.get("messageUrl")
        if (miPushUrl?.isNotEmpty() == true) {
            webview?.clearHistory()
            webview?.loadUrl("${getWebPageUrl()}#/home?url=${miPushUrl}")
            return true
        }

        return false
    }

    private fun getWebPageUrl(): String {
        //如果是一个网址，那就是正式环境，加载bundle
        if (WxPusherConfig.WebUrl.contains("zjiecode.com")) {
            val webDir = WebBundleManager.getWebFileDir()
            return "file://${webDir.absolutePath}/index.html"
        }
        //否则是测试环境，直接加载url
        return WxPusherConfig.WebUrl
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
        //设置容器背景颜色，保持顶部状态栏好看
        val color: Int = resources.getColor(R.color.dark_bg)
        val bgColor = if (night) color else Color.White.toArgb()
        webContainer?.setBackgroundColor(bgColor)

        WxPusherWebInterface.uiModeIsNight = night
        webview?.evaluateJavascript("window.onUiModeChanged && window.onUiModeChanged(${night})") {
        }
    }
}