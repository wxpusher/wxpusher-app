package com.smjcco.wxpusher.push.meizu

import android.content.Context
import android.text.TextUtils
import com.meizu.cloud.pushsdk.MzPushMessageReceiver
import com.meizu.cloud.pushsdk.handler.MzPushMessage
import com.meizu.cloud.pushsdk.platform.message.RegisterStatus
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager

class MeizuPushMsgReceiver : MzPushMessageReceiver() {

    /**
     * 注册状态改变
     */
    override fun onRegisterStatus(p0: Context?, registerStatus: RegisterStatus?) {
        super.onRegisterStatus(p0, registerStatus)
        if (registerStatus != null
            && registerStatus.code == RegisterStatus.SUCCESS_CODE
            && !TextUtils.isEmpty(registerStatus.pushId)
        ) {
            PushManager.onGetPushToken(registerStatus.pushId, DevicePlatform.Android_MEIZU)
        } else {
            PushManager.onGetPushTokenFail(DevicePlatform.Android_MEIZU)
        }
    }

    /**
     * 收到通知，并且应用存活的时候
     */
    override fun onNotificationArrived(p0: Context?, p1: MzPushMessage?) {
        super.onNotificationArrived(p0, p1)
    }
}