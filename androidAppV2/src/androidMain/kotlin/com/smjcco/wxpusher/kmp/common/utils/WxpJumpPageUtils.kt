package com.smjcco.wxpusher.kmp.common.utils

import android.app.Activity
import android.content.Intent
import com.smjcco.wxpusher.kmp.common.withActivity
import com.smjcco.wxpusher.kmp.page.bind.WxpBindActivity
import com.smjcco.wxpusher.kmp.page.login.WxpLoginActivity
import com.smjcco.wxpusher.kmp.page.main.WxpMainActivity
import com.smjcco.wxpusher.kmp.page.unbind.WxpUnbindActivity
import com.smjcco.wxpusher.kmp.page.web.WxpWebViewActivity
import com.smjcco.wxpusher.kmp.page.scan.WxpScanActivity

object WxpJumpPageUtils {

    fun jumpToWebUrl(url: String, activity: Activity? = null) {
        withActivity(activity) {
            val intent = Intent(it, WxpWebViewActivity::class.java);
            intent.putExtra(WxpWebViewActivity.EXTRA_URL, url)
            it.startActivity(intent)
        }
    }

    fun jumpToMain(activity: Activity? = null) {
        withActivity(activity) {
            val intent = Intent(it, WxpMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.startActivity(intent)
        }
    }

    fun jumpToBind(
        phone: String,
        code: String,
        phoneVerifyCode: String,
        activity: Activity? = null
    ) {
        withActivity(activity) {
            WxpBindActivity.start(it, phone, code, phoneVerifyCode)
        }
    }

    fun jumpToLogin(activity: Activity? = null) {
        withActivity(activity) {
            val intent = Intent(it, WxpLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.startActivity(intent)
        }
    }

    fun jumpToUnbind(activity: Activity? = null) {
        withActivity(activity) {
            WxpUnbindActivity.start(it)
        }
    }

    fun jumpToScan(activity: Activity? = null) {
        withActivity(activity) {
            val intent = Intent(it, WxpScanActivity::class.java)
            it.startActivity(intent)
        }
    }

    fun openAppSettings(activity: Activity? = null) {
        withActivity(activity) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", it.packageName, null)
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                it.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}