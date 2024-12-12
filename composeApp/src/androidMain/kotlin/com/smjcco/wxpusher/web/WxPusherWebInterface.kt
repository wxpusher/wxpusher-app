package com.smjcco.wxpusher.web

import android.webkit.JavascriptInterface
import android.widget.Toast
import com.smjcco.wxpusher.utils.ApplicationUtils

/**
 * web服务的接口
 */
object WxPusherWebInterface {
    @JavascriptInterface
    fun showToast(toast: String?) {
        toast?.let {
            Toast.makeText(ApplicationUtils.application, it, Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    fun getDeviceType() = "Android"
}