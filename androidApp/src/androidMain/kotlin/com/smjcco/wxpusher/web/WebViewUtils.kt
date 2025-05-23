package com.smjcco.wxpusher.web

import android.app.Activity
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.widget.AppCompatTextView

object WebViewUtils {
    fun setupView(
        activity: Activity,
        webview: WebView,
        wxPusherWebInterface: WxPusherWebInterface,
        titleTV: AppCompatTextView? = null
    ) {
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

        webview.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                titleTV?.text = title
            }
        }


        webview.webViewClient = WxPusherWebViewClient(activity)

        webview.addJavascriptInterface(wxPusherWebInterface, "wxPusherApi")
    }
}