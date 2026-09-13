package com.smjcco.wxpusher.push.xiaomi

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.GsonUtils
import com.xiaomi.mipush.sdk.ErrorCode
import com.xiaomi.mipush.sdk.MiPushClient
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver

class XiaomiPushMessageReceiver : PushMessageReceiver() {
    private val TAG = "XiaomiPushMessageReceiver"

    override fun onNotificationMessageClicked(context: Context?, message: MiPushMessage?) {
        super.onNotificationMessageClicked(context, message)
        val url = message?.extra?.get("url").orEmpty()
        if (context == null || url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            WxpLogUtils.w(TAG, "点击通知跳转失败，url=$url", e)
        }
    }

    override fun onReceiveRegisterResult(p0: Context?, message: MiPushCommandMessage?) {
        val command = message?.getCommand()
        if (command == null) {
            WxpLogUtils.w(
                TAG,
                "小米获取 pushToken失败，command==null"
            )
            return
        }
        if (!MiPushClient.COMMAND_REGISTER.equals(command)) {
            return
        }
        if (message.resultCode.toInt() == ErrorCode.SUCCESS) {
            val arguments = message.getCommandArguments()
            val regID = arguments?.get(0)
            if (regID.isNullOrEmpty()) {
                WxpLogUtils.w(TAG, "小米获取 pushToken为空1， mRegID=${regID}")
                PushManager.onGetPushTokenFail(DevicePlatform.Android_XIAOMI)
            } else {
                PushManager.onGetPushToken(regID, DevicePlatform.Android_XIAOMI)
            }
        } else {
            WxpLogUtils.w(
                TAG,
                "小米获取 pushToken失败， reason=${message.reason},message=${GsonUtils.toJson(message)}"
            )
            PushManager.onGetPushTokenFail(DevicePlatform.Android_XIAOMI)
        }
    }
}
