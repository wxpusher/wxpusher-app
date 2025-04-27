package com.smjcco.wxpusher

import android.app.Application
import android.os.Build
import android.util.Log
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.notification.NotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.smjcco.wxpusher.ws.IWsMessageListener
import com.smjcco.wxpusher.ws.InitDeviceMsg
import com.smjcco.wxpusher.ws.PushMsgDeviceMsg
import com.smjcco.wxpusher.ws.WsManager
import com.smjcco.wxpusher.ws.WsMessageTypeEnum
import com.tencent.upgrade.bean.UpgradeConfig
import com.tencent.upgrade.core.UpgradeManager
import com.xiaomi.mipush.sdk.MiPushClient


class WxPusherApplication : Application() {
    private val TAG = "AppInit"
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "应用启动")
        ApplicationUtils.application = this
        SaveUtils.init()
        WxPusherConfig.init()
        if (!ApplicationUtils.isMainProcess()) {
            return
        }
        WebBundleManager.init()
        NotificationManager.init()
        initTbs()
        ApplicationUtils.regPushChannel()
    }


    //腾讯应用内升级服务
    private fun initTbs() {
        val builder: UpgradeConfig.Builder = UpgradeConfig.Builder()
        val config = builder.appId("e4aa22fece")
            .appKey("2809e5bc-5ec5-486b-85ba-1d2ec5d5a106")
            .allowDownloadOverMobile(true)
            .systemVersion(Build.VERSION.SDK_INT.toString())
            .userId(AppDataUtils.getLoginInfo()?.uid)
//            .printInternalLog(true)
            .build()
        UpgradeManager.getInstance().init(this, config)

    }

}