package com.smjcco.wxpusher

import android.os.Bundle
import android.webkit.WebView
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import com.smjcco.wxpusher.web.WxPusherWebInterface

class WebViewActivity : ComponentActivity() {
    private val TAG: String = "WebViewActivity"
    lateinit var webview: WebView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_activity)
        initWebView()
        mock()
    }

    private fun initWebView() {
        webview = findViewById(R.id.web)
        webview.settings.javaScriptEnabled = true
        webview.addJavascriptInterface(WxPusherWebInterface, "wxPusherApi")
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

    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return super.getOnBackInvokedDispatcher()
    }

}