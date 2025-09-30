package com.smjcco.wxpusher.kmp.push.xiaomi

import android.content.Context
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.kmp.push.PushManager
import com.smjcco.wxpusher.kmp.common.utils.GsonUtils
import com.xiaomi.mipush.sdk.ErrorCode
import com.xiaomi.mipush.sdk.MiPushClient
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver

class XiaomiPushMessageReceiver : PushMessageReceiver() {
    private val TAG = "XiaomiPushMessageReceiver"
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
            val arguments = message.getCommandArguments();
            val regID = arguments?.get(0);
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