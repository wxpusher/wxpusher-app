package com.smjcco.wxpusher.web

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.utils.WxPusherUtils

class WxPusherWebViewClient(val activity: Activity) : WebViewClient() {
    private val TAG = "WxPusherWebViewClient"
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest?
    ): Boolean {
        val uri = request!!.url
        if (uri.scheme == "http" || uri.scheme == "https"
            || uri.scheme == "about" || uri.scheme == "file"
        ) {
            return false
        }
        //提示打开外部应用
        AlertDialog.Builder(activity)
            .setTitle("是否打开【外部应用】")
            .setMessage("当前链接引导你打开外部应用，是否打开？")
            .setPositiveButton(
                "打开"
            ) { dialog, which ->
                dialog?.dismiss()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    WxPusherLog.w(TAG, "打开特定scheme错误", e)
                    WxPusherUtils.toast("打开${uri.scheme}错误")
                }
            }
            .setCancelable(true)
            .setNegativeButton("取消") { dialog, _ ->
                dialog?.dismiss()
            }
            .create().show()
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        WxPusherLog.e(TAG, "加载页面错误: ${error?.description}")
        super.onReceivedError(view, request, error)
    }
}