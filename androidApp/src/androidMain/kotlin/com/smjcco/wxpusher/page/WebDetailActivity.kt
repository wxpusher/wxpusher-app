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
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.page.web.WebViewUtils
import com.smjcco.wxpusher.page.web.WxPusherWebViewClient
import com.smjcco.wxpusher.push.PushManager
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

    private lateinit var titleTV: AppCompatTextView
    private lateinit var backTV: AppCompatTextView
    private lateinit var optionTV: AppCompatTextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_detail_activity)
        webContainer = findViewById(R.id.web_container)
        initTitleBar()
        enableEdgeToEdge()
        initWebView()
    }

    private fun initTitleBar() {
        titleTV = findViewById(R.id.title)
        backTV = findViewById(R.id.back)
        optionTV = findViewById(R.id.option)
        titleTV.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        PushManager.showOpenNoteRemindSettingDialog(this)
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webview = findViewById(R.id.web)
        wxPusherWebInterface = WxPusherWebInterface(webview)
        WebViewUtils.setupView(this, webview, wxPusherWebInterface, titleTV)
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