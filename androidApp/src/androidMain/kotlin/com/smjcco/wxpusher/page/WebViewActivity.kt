package com.smjcco.wxpusher.page

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.web.WebViewUtils
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.tencent.upgrade.core.DefaultUpgradeStrategyRequestCallback
import com.tencent.upgrade.core.UpgradeManager


class WebViewActivity : ComponentActivity() {
    companion object {
        const val INTENT_KEY_URL = "url"
    }

    private val TAG: String = "WebViewActivity"
    private lateinit var webview: WebView
    private lateinit var wxPusherWebInterface: WxPusherWebInterface
    private var webContainer: View? = null
    private var pressBackTime = System.currentTimeMillis()
    private var preUiMode = -1
    private var hasInit = false;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WxPusherLog.i(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        //避免申请权限的时候，重复调用
        if (hasInit) {
            return
        }
        if (SaveUtils.getByKey(getString(R.string.privacy_key)) != "1") {
            startMainActivity()
            return
        }

        hasInit = true
        setContentView(R.layout.web_activity)
        webContainer = findViewById(R.id.web_container)
        enableEdgeToEdge()
        initWebView()
        requestPermission()
        checkUpdate()
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


    override fun onResume() {
        super.onResume()
        PushManager.showOpenNoteRemindSettingDialog(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webview = findViewById(R.id.web)
        wxPusherWebInterface = WxPusherWebInterface(this, webview)
        WebViewUtils.setupView(this, webview, wxPusherWebInterface)
        // 应用可能的更新
        WebBundleManager.applyUpdateIfAvailable()

        wxPusherWebInterface.onWebLoadFinish = {
            checkNightMode()
        }
        addOnNewIntentListener {
            openPageFromIntent(it)
        }
        //加载页面主体框架
        webview.loadUrl("${getWebPageUrl()}#/home")
        openPageFromIntent(intent)
    }

    /**
     * 如果intent里面有网址，就需要打开具体页面
     */
    private fun openPageFromIntent(intent: Intent?) {
        val url = intent?.getStringExtra(INTENT_KEY_URL)
        if (url.isNullOrEmpty()) {
            WxPusherLog.i(TAG, "跳转url为空")
            return;
        }
        val urlIntent = Intent(this, WebDetailActivity::class.java)
        urlIntent.putExtra(INTENT_KEY_URL, url)
        startActivity(urlIntent)
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

    override fun onPause() {
        super.onPause()
        WxPusherLog.flush()
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
        WxPusherLog.i(TAG, "onUiModeChanged() called with: night = $night")
        //设置容器背景颜色，保持顶部状态栏好看
        val color: Int = resources.getColor(R.color.dark_bg)
        val bgColor = if (night) color else Color.White.toArgb()
        webContainer?.setBackgroundColor(bgColor)

        wxPusherWebInterface.uiModeIsNight = night
        webview?.evaluateJavascript("window.onUiModeChanged && window.onUiModeChanged(${night})") {
        }
    }
}