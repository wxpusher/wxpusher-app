package com.smjcco.wxpusher.kmp.common.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.kmp.common.withActivity
import com.smjcco.wxpusher.kmp.page.bind.WxpBindActivity
import com.smjcco.wxpusher.kmp.page.login.WxpLoginActivity
import com.smjcco.wxpusher.kmp.page.main.WxpMainActivity
import com.smjcco.wxpusher.kmp.page.scan.WxpScanActivity
import com.smjcco.wxpusher.kmp.page.unbind.WxpUnbindActivity
import com.smjcco.wxpusher.kmp.page.useragreement.WxpUserAgreementActivity
import com.smjcco.wxpusher.kmp.page.web.WxpWebViewActivity
import com.smjcco.wxpusher.kmp.push.ws.WxpNotificationManager


object WxpJumpPageUtils {

    fun jumpToSystemAppSettings(activity: Activity? = null) {
        withActivity(activity) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", it.packageName, null)
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                it.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 打开通知设置页面
     */
    fun jumpToSystemNotificationSettingPage(activity: Activity? = null) {
        withActivity(activity) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(
                Settings.EXTRA_APP_PACKAGE,
                ApplicationUtils.getApplication().getPackageName()
            )
            intent.putExtra(
                Settings.EXTRA_CHANNEL_ID,
                WxpNotificationManager.WxPusherSystemChannelId
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            it.startActivity(intent)
        }
    }

    /**
     * 跳转到电池优化设置
     */
    @SuppressLint("BatteryLife")
    fun jumpToSystemIgnoreBatteryOptimizationSettings(activity: Activity? = null) {
        withActivity(activity) {
//            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
//            it.startActivity(intent)
            //下面这个跳转更加精确，原生安卓可以直接弹出申请弹窗
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.fromParts("package", it.packageName, null)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            it.startActivity(intent)
        }
    }

    fun jumpToSystemAlarmSettings(activity: Activity? = null) {
        //低版本不支持
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        withActivity(activity) {
            //下面这个跳转更加精确，原生安卓可以直接弹出申请弹窗
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.fromParts("package", it.packageName, null)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            it.startActivity(intent)
        }
    }

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

    fun jumpToUserAgreement(activity: Activity? = null) {
        withActivity(activity) {
            WxpUserAgreementActivity.start(it)
        }
    }

}