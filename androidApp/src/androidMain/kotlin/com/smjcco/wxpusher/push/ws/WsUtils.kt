package com.smjcco.wxpusher.push.ws

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.page.CheckActivity
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils

object WsUtils {
    private val SAVE_SHOW_KEY = "alertTips"

    /**
     * 提示保活
     */
    fun showSettingGuide(activity: Activity) {
        if (SaveUtils.getByKey("noteKeepAlive") == "1") {
            return
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("保活提示")
            .setMessage("由于Android的系统限制，应用在后台会被限制运行，导致收不到消息，请打开后台限制。")
            .setPositiveButton(
                "去设置"
            ) { dialog, which ->
                dialog?.dismiss()
                val intent = Intent(ApplicationUtils.application, CheckActivity::class.java)
                activity.startActivity(intent)
            }
            .setCancelable(false)
            .setNegativeButton("不再提醒") { dialog, _ ->
                dialog?.dismiss()
                SaveUtils.setKeyValue("noteKeepAlive", "1")
            }
            .create()
        DialogManager.show(activity, dialog)
    }
}