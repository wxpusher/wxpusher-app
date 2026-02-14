package com.smjcco.wxpusher.app

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.IWxpLoading
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.utils.ThreadUtils

class WxpLoadingServiceImpl : IWxpLoading {
    private var loadingDialog: Dialog? = null

    override fun showLoading(msg: String?, canDismiss: Boolean) {
        ThreadUtils.runOnMainThread {
            try {
                val activity = ApplicationUtils.getCurrentActivity()
                if (activity == null || activity.isFinishing || activity.isDestroyed) {
                    return@runOnMainThread
                }

                dismissInternal()

                val dialog = Dialog(activity, R.style.LoadingDialogTheme)
                val view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null)
                dialog.setContentView(view)

                val messageView = view.findViewById<TextView>(R.id.loading_message)
                if (msg.isNullOrEmpty()) {
                    messageView.visibility = View.GONE
                } else {
                    messageView.visibility = View.VISIBLE
                    messageView.text = msg
                }

                dialog.setCancelable(canDismiss)
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()
                loadingDialog = dialog
            } catch (e: Exception) {
                WxpLogUtils.w(message = "弹出loading错误", throwable = e)
            }
        }
    }

    override fun dismissLoading() {
        ThreadUtils.runOnMainThread {
            dismissInternal()
        }
    }

    private fun dismissInternal() {
        try {
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }
        } catch (e: Exception) {
            WxpLogUtils.w(message = "dismissInternal错误", throwable = e)
        } finally {
            loadingDialog = null
        }
    }
}
