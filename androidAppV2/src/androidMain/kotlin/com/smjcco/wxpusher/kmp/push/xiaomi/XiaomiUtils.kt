package com.smjcco.wxpusher.kmp.push.xiaomi

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.kmp.common.utils.WxpJumpPageUtils
import com.smjcco.wxpusher.kmp.push.PushManager
import com.smjcco.wxpusher.kmp.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.utils.PermissionUtils
import com.xiaomi.mipush.sdk.MiPushClient

object XiaomiUtils {
    private val SAVE_SHOW_KEY = "Xiaomi_alertTips"

    //本地打开应用是否已经提示过了
    private var hasShow = false

    fun init(application: Application) {
        MiPushClient.registerPush(
            application,
            "2882303761520373007",
            "5932037320007"
        )
    }

    /**
     * 提示保活
     */
    fun showSettingGuide(activity: Activity) {
        if (WxpSaveService.get(SAVE_SHOW_KEY, "") == "1") {
            return
        }
        if (hasShow) {
            return
        }
        //针对小米，还没有创建推送通道 ，就不进行提醒
        //在收到一次消息以后，才会有这个通知通道
        if (!WxpNotificationManager.hasNotificationChannel("mipush|com.smjcco.wxpusher|135072")) {
            return
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("请打开通知提醒设置")
            .setMessage("如果通知不提醒，不弹窗，按照如下2步操作：\n\n1、请点击「去设置」，【打开所有开关】\n\n2、在设置最下面选择「订阅消息」-【打开所有开关】，在设置里，你还可以自定义提示铃声。")
            .setNegativeButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                PermissionUtils.gotoNotificationSettingPage()
                hasShow = false
            }
            .setCancelable(false)
            .setPositiveButton("查看视频教程") { dialog, _ ->
                dialog?.dismiss()
                WxpJumpPageUtils.jumpToWebUrl(PushManager.getGuidePageUrl(), activity)
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