package com.smjcco.wxpusher.page

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.dialog.ActionSheetDialogFragment
import com.smjcco.wxpusher.dialog.ActionSheetItem
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import com.smjcco.wxpusher.web.WebViewUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface


class WebDetailActivity : FragmentActivity() {
    companion object {
        const val INTENT_KEY_URL = "url"
        fun openUrl(activity: Activity, url: String) {
            val intent = Intent(activity, WebDetailActivity::class.java)
            intent.putExtra(INTENT_KEY_URL, url)
            activity.startActivity(intent)
        }
    }

    private val TAG: String = "WebDetailActivity"
    private lateinit var webview: WebView
    private lateinit var wxPusherWebInterface: WxPusherWebInterface
    private var webContainer: View? = null
    private var pressBackTime = System.currentTimeMillis()
    private var preUiMode = -1

    private lateinit var titleTV: AppCompatTextView
    private lateinit var backView: View
    private lateinit var optionView: View


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
        backView = findViewById(R.id.back)
        optionView = findViewById(R.id.option)

        backView.setOnClickListener {
            onBackPressed()
        }
        optionView.setOnClickListener {
            val url = webview.url ?: (intent?.getStringExtra(INTENT_KEY_URL) ?: "")
            val items = listOf(
                ActionSheetItem("在浏览器中打开") {
                    if (url.isEmpty()) {
                        WxPusherLog.w(TAG, "在浏览器中打开失败，url=null")
                        return@ActionSheetItem
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ApplicationUtils.application.startActivity(intent)
                    } catch (e: Exception) {
                        WxPusherLog.w(TAG, "打开浏览器失败: ${e.message}")
                        WxPusherUtils.toast("打开浏览器失败")
                    }
                },
                ActionSheetItem("复制链接") {
                    if (url.isEmpty()) {
                        WxPusherLog.w(TAG, "复制失败，url=null")
                        return@ActionSheetItem
                    }
                    val clipboardManager =
                        ApplicationUtils.application.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = android.content.ClipData.newPlainText("WxPusher", url)
                    clipboardManager.setPrimaryClip(clipData)
                    WxPusherUtils.toast("复制成功")
                },
                ActionSheetItem("分享") {
                    if (url.isEmpty()) {
                        WxPusherLog.w(TAG, "复制失败，url=null")
                        return@ActionSheetItem
                    }
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, url)
                    val chooser = Intent.createChooser(intent, "分享到")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ApplicationUtils.application.startActivity(chooser)
                },
                ActionSheetItem("重新加载") {
                    if (url.isEmpty()) {
                        WxPusherLog.w(TAG, "重新加载，url=null")
                        return@ActionSheetItem
                    }
                    webview.reload()
                }
            )
            ActionSheetDialogFragment(listOf(items)).show(supportFragmentManager, "action_sheet")
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

    override fun onDestroy() {
        super.onDestroy()
        if (webview.isActivated) {
            webview.destroy()
        }
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