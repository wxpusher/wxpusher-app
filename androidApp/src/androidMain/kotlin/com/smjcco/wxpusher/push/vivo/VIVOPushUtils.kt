package com.smjcco.wxpusher.push.vivo

import android.app.Application
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.PushManager
import com.vivo.push.PushClient
import com.vivo.push.PushConfig
import com.vivo.push.listener.IPushQueryActionListener


object VIVOPushUtils {
    private final val TAG = "VIVO"
    fun init(application: Application) {
        //初始化设置
        val config = PushConfig.Builder()
            .agreePrivacyStatement(true)
            .build()
        PushClient.getInstance(application).initialize(config)

        //获取pushToken
        PushClient.getInstance(application)
            .getRegId(object : IPushQueryActionListener {
                override fun onSuccess(regId: String?) {
                    WxPusherLog.w(TAG, "VIVO获取pushToken获取结果,regId=${regId}")
                    if (regId.isNullOrEmpty()) {
                        WxPusherLog.w(TAG, "VIVO获取pushToken失败,regId==null")
                        PushManager.onGetPushTokenFail(DevicePlatform.Android_VIVO)
                    } else {
                        PushManager.onGetPushToken(regId, DevicePlatform.Android_VIVO)
                    }
                }

                override fun onFail(state: Int) {
                    WxPusherLog.w(TAG, "VIVO获取pushToken失败$state")
                    PushManager.onGetPushTokenFail(DevicePlatform.Android_VIVO)
                }
            })
        //打开推送
        PushClient.getInstance(application).turnOnPush { state ->
            if (state == 0) {
                WxPusherLog.w(TAG, "打开VIVO push成功")
            } else {
                WxPusherLog.w(TAG, "打开VIVO push异常[$state]")
            }
        }
    }
}