package com.smjcco.wxpusher.base.common

import android.app.AlertDialog

actual fun WxpDialogUtils_showDialog(params: WxpDialogParams) {
    try {
        val activity = ApplicationUtils.getCurrentActivity()
        if (activity == null) {
            WxpLogUtils.w("dialog", "没有activity，无法显示dialog")
            return
        }
        val builder = AlertDialog.Builder(activity)

        params.title?.let { builder.setTitle(it) }
        params.message?.let { builder.setMessage(it) }

        params.leftText?.let { leftText ->
            if (leftText.isNotEmpty()) {
                builder.setNegativeButton(leftText) { dialog, _ ->
                    dialog.dismiss()
                    params.leftBlock?.invoke()
                }
            }
        }

        params.rightText?.let { rightText ->
            if (rightText.isNotEmpty()) {
                builder.setPositiveButton(rightText) { dialog, _ ->
                    dialog.dismiss()
                    params.rightBlock?.invoke()
                }
            }
        }
        builder.setCancelable(params.cancelable)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(params.cancelable)
        dialog.show()
    } catch (e: Exception) {
        WxpLogUtils.w("dialog", "显示dialog出现错误", e)
    }
}
