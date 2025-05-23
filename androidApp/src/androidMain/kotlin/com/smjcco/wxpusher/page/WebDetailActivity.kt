package com.smjcco.wxpusher.page

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.page.web.WxPusherWebViewClient
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface


class WebDetailActivity : ComponentActivity() {
    companion object {
        const val INTENT_KEY_URL = "url"
    }

    private val TAG: String = "WebDetailActivity"
    private lateinit var webview: WebView
    private lateinit var wxPusherWebInterface: WxPusherWebInterface
    private var webContainer: View? = null
    private var pressBackTime = System.currentTimeMillis()
    private var preUiMode = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_detail_activity)
        webContainer = findViewById(R.id.web_container)
        enableEdgeToEdge()
        initWebView()
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
            .setMessage("由于Android的系统限制，应用在后台会被限制运行，导致收不到消息，请打开后台限制。")
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
        //自建通道需要提醒保活
        if (AppDataUtils.getPushToken()?.startsWith("PT_") == true) {
            noteKeepAlive()
        } else {
            showSettingGuide()
        }
    }

    /**
     * 提示保活
     */
    private fun showSettingGuide() {
        //没有登录不提醒
        if (AppDataUtils.getLoginInfo()?.deviceToken.isNullOrEmpty()) {
            return
        }
        //针对小米，还没有创建推送通道 ，就不进行提醒
        if (DeviceUtils.isMIUI() && !NotificationManager.hasNotificationChannel("mipush|com.smjcco.wxpusher|135072")) {
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
        wxPusherWebInterface = WxPusherWebInterface(webview)
        webview.settings.apply {
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

        webview.webChromeClient = WebChromeClient()

        webview.webViewClient = WxPusherWebViewClient(this)

        webview.addJavascriptInterface(wxPusherWebInterface, "wxPusherApi")

        wxPusherWebInterface.onWebLoadFinish = {
            checkNightMode()
        }
        addOnNewIntentListener {
            webview.clearHistory()
            openPageFromIntent(it)
        }

        webview.clearHistory()
        if (!openPageFromIntent(intent)) {
            // TODO: 没有传入数据，打开一个默认页面
//            webview.lo
        }
    }

    private fun openPageFromIntent(intent: Intent?): Boolean {
        val url = intent?.getStringExtra(INTENT_KEY_URL)
        if (!url.isNullOrEmpty()) {
            webview.loadUrl(url)
            return true
        }
        return false
    }


    override fun onBackPressed() {
        if (System.currentTimeMillis() - pressBackTime < 400) {
            //连续点击返回，直接返回页面
            finish()
            return
        }
        pressBackTime = System.currentTimeMillis()
        if (webview.canGoBack() == true) {
            webview.goBack()
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
        WxPusherLog.i(TAG, "onUiModeChanged() called with: night = $night")
        //设置容器背景颜色，保持顶部状态栏好看
        val color: Int = resources.getColor(R.color.dark_bg)
        val bgColor = if (night) color else Color.White.toArgb()
        webContainer?.setBackgroundColor(bgColor)

        wxPusherWebInterface.uiModeIsNight = night
        webview.evaluateJavascript("window.onUiModeChanged && window.onUiModeChanged(${night})") {
        }
    }
}