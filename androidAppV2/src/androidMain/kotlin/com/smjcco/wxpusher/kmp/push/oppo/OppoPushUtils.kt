package com.smjcco.wxpusher.kmp.push.oppo

import android.app.Application
import com.heytap.msp.push.HeytapPushManager
import com.heytap.msp.push.callback.ICallBackResultService
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.kmp.push.PushManager
import com.smjcco.wxpusher.log.WxPusherLog

object OppoPushUtils {
    private val TAG = "Oppo"
    fun init(application: Application) {
        HeytapPushManager.init(application, true)
        HeytapPushManager.register(
            application,
            "a9f0717ade814f13a449f19c8a6800b4",
            "48490dd400a340d9b4049b3d11f950c7",
            object : ICallBackResultService {
                override fun onRegister(
                    responseCode: Int,
                    registerID: String?,
                    packageName: String?,
                    miniPackageName: String?
                ) {
                    if (responseCode != 0) {
                        WxPusherLog.w(
                            TAG,
                            "OPPO推送-注册错误，onRegister()， responseCode = $responseCode"
                        )
                        PushManager.onGetPushTokenFail(DevicePlatform.Android_OPPO)
                    } else if (registerID.isNullOrEmpty()) {
                        WxPusherLog.w(
                            TAG,
                            "OPPO推送- token为空，responseCode = $responseCode"
                        )
                        PushManager.onGetPushTokenFail(DevicePlatform.Android_OPPO)
                    } else {
                        PushManager.onGetPushToken(registerID, DevicePlatform.Android_OPPO)
                    }
                }

                override fun onUnRegister(p0: Int, p1: String?, p2: String?) {
                }

                override fun onSetPushTime(p0: Int, p1: String?) {
                }

                override fun onGetPushStatus(p0: Int, p1: Int) {
                }

                override fun onGetNotificationStatus(p0: Int, p1: Int) {
                }

                override fun onError(p0: Int, p1: String?, p2: String?, p3: String?) {
                    WxPusherLog.w(
                        TAG,
                        "OPPO推送错误，onError() called with: p0 = $p0, p1 = $p1, p2 = $p2, p3 = $p3"
                    )
                }
            })
    }
}