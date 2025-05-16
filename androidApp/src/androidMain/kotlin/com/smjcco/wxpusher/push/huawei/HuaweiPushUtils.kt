package com.smjcco.wxpusher.push.huawei

import android.app.Application
import android.text.TextUtils
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.WxPusherUtils
import kotlinx.coroutines.launch

object HuaweiPushUtils {
    private  val TAG = "Huawei"
    fun init(application: Application) {
        WxPusherUtils.getIoScopeScope().launch {
            try {
                val token = HmsInstanceId.getInstance(application)
                    .getToken("114073793", "HCM")
                if (!TextUtils.isEmpty(token)) {
                    WxPusherLog.i(TAG, "HuaweiPush 获取token=${token}")
                    PushManager.onGetPushToken(token, DevicePlatform.Android_HUAWEI)
                } else {
                    WxPusherLog.i(TAG, "HuaweiPush 获取token失败，为空")
                    PushManager.onGetPushTokenFail(DevicePlatform.Android_HUAWEI)
                }
            } catch (e: ApiException) {
                WxPusherLog.e(TAG, "HuaweiPush 获取token失败", e)
                PushManager.onGetPushTokenFail(DevicePlatform.Android_HUAWEI)
            }
        }
    }
}