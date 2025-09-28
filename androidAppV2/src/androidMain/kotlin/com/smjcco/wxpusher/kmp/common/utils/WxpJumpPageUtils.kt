package com.smjcco.wxpusher.kmp.common.utils

import android.app.Activity
import android.content.Intent
import com.smjcco.wxpusher.kmp.common.withActivity
import com.smjcco.wxpusher.kmp.page.bind.WxpBindActivity
import com.smjcco.wxpusher.kmp.page.login.WxpLoginActivity
import com.smjcco.wxpusher.kmp.page.main.WxpMainActivity
import com.smjcco.wxpusher.kmp.page.unbind.WxpUnbindActivity
import com.smjcco.wxpusher.kmp.page.web.WxpWebViewActivity

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
}