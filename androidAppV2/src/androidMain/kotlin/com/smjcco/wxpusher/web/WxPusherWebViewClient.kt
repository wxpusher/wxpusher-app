package com.smjcco.wxpusher.web

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.utils.WxPusherUtils

class WxPusherWebViewClient(
    val activity: Activity,
    val progress: ProgressBar?,
    val wxPusherWebInterface: WxPusherWebInterface,
) : WebViewClient() {
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
        val dialog = AlertDialog.Builder(activity)
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
                    WxpLogUtils.w(TAG, "打开特定scheme错误", e)
                    WxPusherUtils.toast("打开${uri.scheme}错误")
                }
            }
            .setCancelable(true)
            .setNegativeButton("取消") { dialog, _ ->
                dialog?.dismiss()
            }
            .create()
        DialogManager.show(activity, dialog)
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        wxPusherWebInterface.webUrl = url
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        WxpLogUtils.e(TAG, "加载页面错误: ${error?.description}")
        super.onReceivedError(view, request, error)
    }

}