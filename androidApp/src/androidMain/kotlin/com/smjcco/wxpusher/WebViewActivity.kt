package com.smjcco.wxpusher

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.web.WxPusherWebInterface
import com.smjcco.wxpusher.ws.WsManager

class WebViewActivity : ComponentActivity(), WsManager.IWsConnectChangedListener {
    private val TAG: String = "WebViewActivity"
    lateinit var webview: WebView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_activity)
        initWebView()
        mock()
        requestPermission()
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
        webview.settings.javaScriptEnabled = true
        webview.addJavascriptInterface(WxPusherWebInterface, "wxPusherApi")
        //链接成功，需要调用到容器，上报一下
        WsManager.addConnectChangedListener(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        WsManager.removeConnectChangedListener(this)
    }

    private fun mock() {
        webview.loadUrl("http://10.0.0.10:3000/home")
//        web.loadUrl("http://10.0.0.10:3000/login/bind")
//        web.loadUrl("https://m.baidu.com")
    }

    override fun onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack()
            return
        }
        super.onBackPressed()
    }


    //把ws链接状态通知到容器里面，让容器再上报一次，避免pushToken变更，没有及时上报到服务器
    override fun onChanged(connectStatus: Boolean) {
        webview.evaluateJavascript("window.onWsConnect && window.onWsConnect(${connectStatus})") {

        }
    }

}