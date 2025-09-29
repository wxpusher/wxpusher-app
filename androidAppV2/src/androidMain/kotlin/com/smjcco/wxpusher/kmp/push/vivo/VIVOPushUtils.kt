package com.smjcco.wxpusher.kmp.push.vivo

import android.app.Application
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.kmp.push.PushManager
import com.smjcco.wxpusher.log.WxPusherLog
import com.vivo.push.PushClient
import com.vivo.push.PushConfig
import com.vivo.push.listener.IPushQueryActionListener


object VIVOPushUtils {
    private val TAG = "VIVO"
    private val TurnOnPushSaveKey = "Vivo-turnOnPush"

    fun init(application: Application) {
        try {
            //初始化设置
            val config = PushConfig.Builder()
                .agreePrivacyStatement(true)
                .build()
            PushClient.getInstance(application).initialize(config)

            //已经打开过推送了，直接获取pushToken
            if (WxpSaveService.get(TurnOnPushSaveKey, "") == "true") {
                getPushToken(application)
            } else {
                //第一次打开的时候 ，先打开推送，不然获取pushToken 可能为空
                PushClient.getInstance(application).turnOnPush { state ->
                    if (state == 0) {
                        WxpSaveService.set(TurnOnPushSaveKey, "true")
                        WxPusherLog.w(TAG, "打开VIVO push成功")
                        getPushToken(application)
                    } else {
                        WxPusherLog.w(TAG, "打开VIVO push异常[$state]")
                        PushManager.onGetPushTokenFail(DevicePlatform.Android_VIVO)
                    }
                }
            }
        } catch (e: Throwable) {
            WxPusherLog.w(TAG, "VIVO推送初始化错误", e)
        }
    }

    private fun getPushToken(application: Application) {
        //获取pushToken
        PushClient.getInstance(application)
            .getRegId(object : IPushQueryActionListener {
                override fun onSuccess(regId: String?) {
                    WxPusherLog.w(TAG, "VIVO获取pushToken获取结果,regId=${regId}")
                    if (regId.isNullOrEmpty()) {
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
    }

}