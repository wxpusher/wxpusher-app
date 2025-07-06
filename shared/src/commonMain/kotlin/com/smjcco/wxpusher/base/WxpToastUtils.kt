package com.smjcco.wxpusher.base

expect fun ExpWxpToastUtils_showToast(msg: String)

object WxpToastUtils {

    fun showToast(msg: String?) {
        if (msg.isNullOrEmpty()) {
            return
        }
        runAtMainSuspend {
            ExpWxpToastUtils_showToast(msg)
        }
    }
}