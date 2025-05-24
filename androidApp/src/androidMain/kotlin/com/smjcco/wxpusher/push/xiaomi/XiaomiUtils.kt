package com.smjcco.wxpusher.push.xiaomi

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.page.WebDetailActivity
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.SaveUtils
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
        if (SaveUtils.getByKey(SAVE_SHOW_KEY) == "1") {
            return
        }
        if (hasShow) {
            return
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("请打开通知提醒设置")
            .setMessage("如果通知不提醒，不弹窗，请点击「去设置」，选择「订阅消息」-「在锁定屏幕上」设置为【显示通知】\n\n在设置里，你还可以自定义提示铃声。")
            .setNegativeButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                PermissionUtils.gotoNotificationSettingPage()
            }
            .setCancelable(false)
            .setPositiveButton("查看视频教程") { dialog, _ ->
                dialog?.dismiss()
                WebDetailActivity.openUrl(activity, PushManager.getGuidePageUrl())
            }
            .setNeutralButton("永不提醒") { dialog, _ ->
                dialog?.dismiss()
                SaveUtils.setKeyValue(SAVE_SHOW_KEY, "1")
            }
            .create()
        hasShow = true
        DialogManager.show(activity, dialog)

    }

}