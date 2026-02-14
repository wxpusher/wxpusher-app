package com.smjcco.wxpusher.push.meizu

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import com.meizu.cloud.pushsdk.PushManager
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.utils.PermissionUtils

object MeizuPushUtils {
    private val SAVE_SHOW_KEY = "Meizu_alertTips"
    private val appId = "155986"
    private val appKey = "b8bd81b459d840779feea5b87d33714f"
    private var hasShow = false

    /**
     * 初始化魅族推送
     */
    fun init(application: Application) {
        WxpLogUtils.i("MEIZU", "魅族SDK version=" + PushManager.getSdkVersion())
        PushManager.register(application, appId, appKey);
    }

    fun openPushSwitch(token: String?) {
        PushManager.switchPush(
            ApplicationUtils.getApplication(),
            appId,
            appKey,
            token,
            true
        )
    }

    fun showSettingGuide(activity: Activity) {
        if (WxpSaveService.get(SAVE_SHOW_KEY, "") == "1") {
            return
        }
        if (hasShow) {
            return
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("请打开通知提醒设置")
            .setMessage("如果通知不提醒，不弹窗，请按如下步骤进行设置：\n\n1、请点击「去设置」，勾选所有提醒方。\n\n你也可以稍后在【我的】-【通知设置】里面检查")
            .setNegativeButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                PermissionUtils.gotoNotificationSettingPage()
                hasShow = false
            }
            .setCancelable(false)
            .setPositiveButton("关闭") { dialog, _ ->
                dialog?.dismiss()
                hasShow = false
            }
            .setNeutralButton("永不提醒") { dialog, _ ->
                dialog?.dismiss()
                WxpSaveService.set(SAVE_SHOW_KEY, "1")
                hasShow = false
            }
            .create()
        hasShow = true
        DialogManager.show(activity, dialog)

    }
}