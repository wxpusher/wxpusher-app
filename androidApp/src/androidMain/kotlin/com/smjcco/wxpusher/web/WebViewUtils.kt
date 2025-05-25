package com.smjcco.wxpusher.web

import android.app.Activity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import com.smjcco.wxpusher.BuildConfig

object WebViewUtils {
    fun setupView(
        activity: Activity,
        webview: WebView,
        wxPusherWebInterface: WxPusherWebInterface,
        progress: ProgressBar? = null,
        titleTV: AppCompatTextView? = null
    ) {
        if (!BuildConfig.online) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
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

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progress?.visibility = View.VISIBLE
                progress?.setProgress(newProgress, true)
                if (progress != null && newProgress >= 100) {
                    progress?.visibility = View.INVISIBLE
                }
            }
        }


        webview.webViewClient = WxPusherWebViewClient(activity, progress, wxPusherWebInterface)

        webview.addJavascriptInterface(wxPusherWebInterface, "wxPusherApi")
    }
}