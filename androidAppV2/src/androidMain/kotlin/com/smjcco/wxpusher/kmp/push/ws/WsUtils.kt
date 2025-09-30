package com.smjcco.wxpusher.kmp.push.ws

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.kmp.common.utils.WxpJumpPageUtils

object WsUtils {
    private val SAVE_SHOW_KEY = "ws_keep_alive_tips_dialog"

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
            .setTitle("保持后台运行提醒")
            .setMessage("由于Android的系统限制，需要给应用后台运行的权限才可以接收消息，请你关闭电池优化或者关闭后台运行限制。")
            .setPositiveButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                WxpJumpPageUtils.jumpToSystemIgnoreBatteryOptimizationSettings()
            }
            .setCancelable(false)
            .setNegativeButton("关闭") { dialog, _ ->
                dialog?.dismiss()
            }
            .setNeutralButton("不再提醒") { dialog, _ ->
                dialog?.dismiss()
                WxpSaveService.set(SAVE_SHOW_KEY, "1")
            }
            .create()
        hasShow = true
        DialogManager.show(activity, dialog)
    }
}