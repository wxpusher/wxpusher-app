package com.smjcco.wxpusher.push.honor

import com.hihonor.push.sdk.HonorMessageService
import com.hihonor.push.sdk.HonorPushCallback
import com.hihonor.push.sdk.HonorPushClient
import com.hihonor.push.sdk.HonorPushDataMsg
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager


class HonorPushMessageService : HonorMessageService() {
    private val TAG = "Honor"
    override fun onNewToken(s: String?) {
        if (s.isNullOrEmpty()) {
            WxpLogUtils.w(TAG, "荣耀推送-onNewToken=null")
            return
        }
        WxpLogUtils.i(TAG, "荣耀推送-通过HonorPushMessageService获取token")
        PushManager.onGetPushToken(s, DevicePlatform.Android_HONOR)

        HonorPushClient.getInstance().turnOnNotificationCenter(object : HonorPushCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                WxpLogUtils.i(TAG, "荣耀推送-打开通知烂推送成功")
            }

            override fun onFailure(errorCode: Int, errorString: String) {
                WxpLogUtils.w(
                    TAG,
                    "荣耀推送-turnOnNotificationCenter失败，errorCode=$errorCode,errorString=$errorString"
                )
            }
        })
    }

    override fun onMessageReceived(msg: HonorPushDataMsg?) {
    }
}