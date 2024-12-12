package com.smjcco.wxpusher

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import com.smjcco.wxpusher.web.WxPusherWebInterface

class WebViewActivity : ComponentActivity() {
    private val TAG: String = "WebViewActivity"
    lateinit var webview: WebView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_activity)
        init()
        mock()
    }

    private fun init() {
        webview = findViewById(R.id.web)
        webview.settings.javaScriptEnabled = true
        webview.addJavascriptInterface(WxPusherWebInterface, "wxPusherApi")
//        web.webViewClient=object : WebViewClient(){
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//            }
//
//            override fun onReceivedError(
//                view: WebView?,
//                errorCode: Int,
//                description: String?,
//                failingUrl: String?
//            ) {
//
//                Log.d(
//                    TAG,
//                    "onReceivedError() called with: view = $view, errorCode = $errorCode, description = $description, failingUrl = $failingUrl"
//                )
//            }
//
//            override fun onReceivedError(
//                view: WebView?,
//                request: WebResourceRequest?,
//                error: WebResourceError?
//            ) {
//                Log.d(
//                    TAG,
//                    "onReceivedError() called with: view = $view, request = $request, error = $error"
//                )
//            }
//
//            override fun onReceivedHttpError(
//                view: WebView?,
//                request: WebResourceRequest?,
//                errorResponse: WebResourceResponse?
//            ) {
//                Log.d(
//                    TAG,
//                    "onReceivedHttpError() called with: view = $view, request = $request, errorResponse = $errorResponse"
//                )
//            }
//        }
    }

    private fun mock() {
        webview.loadUrl("http://10.0.0.10:3000/login/login")
//        web.loadUrl("http://10.0.0.10:3000/login/bind")
//        web.loadUrl("https://m.baidu.com")
    }

}