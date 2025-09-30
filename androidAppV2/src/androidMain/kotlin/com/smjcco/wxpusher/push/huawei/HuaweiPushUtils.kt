package com.smjcco.wxpusher.push.huawei

import android.app.Application
import android.text.TextUtils
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushManager
import kotlinx.coroutines.launch

object HuaweiPushUtils {
    private val TAG = "Huawei"
    fun init(application: Application) {
        HmsMessaging.getInstance(application).isAutoInitEnabled = true
        WxpScopeUtils.getIoScopeScope().launch {
            try {
                val token = HmsInstanceId.getInstance(application)
                    .getToken("114073793", "HCM")
                if (!TextUtils.isEmpty(token)) {
                    WxpLogUtils.i(TAG, "HuaweiPush 获取token=${token}")
                    PushManager.onGetPushToken(token, DevicePlatform.Android_HUAWEI)
                } else {
                    WxpLogUtils.i(TAG, "HuaweiPush 获取token失败，为空")
                    PushManager.onGetPushTokenFail(DevicePlatform.Android_HUAWEI)
                }
            } catch (e: ApiException) {
                WxpLogUtils.e(TAG, "HuaweiPush 获取token失败", e)
                PushManager.onGetPushTokenFail(DevicePlatform.Android_HUAWEI)
            }
        }
    }
}