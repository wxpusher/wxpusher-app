package com.smjcco.wxpusher.push.xiaomi

import android.content.Context
import android.util.Log
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.utils.AppDataUtils
import com.xiaomi.mipush.sdk.ErrorCode
import com.xiaomi.mipush.sdk.MiPushClient
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver

class XiaomiPushMessageReceiver : PushMessageReceiver() {
    private val TAG = "XiaomiPushMessageReceiver"
    override fun onReceiveRegisterResult(p0: Context?, message: MiPushCommandMessage?) {
        val command = message?.getCommand();
        if (command == null || !MiPushClient.COMMAND_REGISTER.equals(command)) {
            return
        }
        val arguments = message.getCommandArguments();
        if (message.getResultCode().toInt() == ErrorCode.SUCCESS) {
            val regID = arguments?.get(0);
            Log.d(TAG, "收到MIUI mRegID=${regID}")
            AppDataUtils.savePushToken(regID)
            DeviceApi.updateDeviceInfoAsync()
        }
    }
}