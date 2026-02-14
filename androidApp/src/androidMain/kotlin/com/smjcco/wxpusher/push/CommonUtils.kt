package com.smjcco.wxpusher.push

import android.app.Activity
import android.app.AlertDialog
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

object CommonUtils {

    private val SAVE_SHOW_KEY = "Common_alertTips"

    //本地打开应用是否已经提示过了
    private var hasShow = false

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
        val dialog = AlertDialog.Builder(activity)
            .setTitle("请打开通知提醒设置")
            .setMessage("如果通知不提醒，不弹窗，请你根据视频教程进行设置，你还可以自定义提示铃声，避免消息遗漏。")
            .setNegativeButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                PermissionUtils.gotoNotificationSettingPage()
            }
            .setCancelable(false)
            .setPositiveButton("查看视频教程") { dialog, _ ->
                dialog?.dismiss()
                WxpJumpPageUtils.jumpToWebUrl(PushManager.getGuidePageUrl(), activity)
            }
            .setNeutralButton("永不提醒") { dialog, _ ->
                dialog?.dismiss()
                WxpSaveService.set(SAVE_SHOW_KEY, "1")
            }
            .create()
        hasShow = true
        DialogManager.show(activity, dialog)

    }
}