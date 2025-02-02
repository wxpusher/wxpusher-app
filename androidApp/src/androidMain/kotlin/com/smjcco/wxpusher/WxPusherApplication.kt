package com.smjcco.wxpusher

import android.app.Application
import android.os.Build
import android.util.Log
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.notification.NotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.smjcco.wxpusher.ws.IWsMessageListener
import com.smjcco.wxpusher.ws.InitDeviceMsg
import com.smjcco.wxpusher.ws.KeepWsConnectService
import com.smjcco.wxpusher.ws.PushMsgDeviceMsg
import com.smjcco.wxpusher.ws.WsManager
import com.smjcco.wxpusher.ws.WsMessageTypeEnum
import com.tencent.upgrade.bean.UpgradeConfig
import com.tencent.upgrade.core.UpgradeManager


class WxPusherApplication : Application() {
    private val TAG = "AppInit"
    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.application = this
        SaveUtils.init()
        initBiz()
        WsManager.init()
        NotificationManager.init()
        KeepWsConnectService.start(this)
        WebBundleManager.init()
        initTbs()
    }

    private fun initBiz() {
        //当收到消息的时候，发送到通知栏
        val pushListener = object : IWsMessageListener<PushMsgDeviceMsg> {
            override fun onMessage(message: PushMsgDeviceMsg) {
                sendBizMessageNotification(message)
            }
        }
        WsManager.addMsgListener(WsMessageTypeEnum.PUSH_NOTE.code, pushListener)

        val pushTokenListener = object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) {
                AppDataUtils.savePushToken(message.pushToken)
                Log.d(TAG, "收到长链接Ws的pushToken=${message.pushToken}")
                DeviceApi.updateDeviceInfoAsync()
            }
        }
        WsManager.addMsgListener(WsMessageTypeEnum.DEVICE_INIT.code, pushTokenListener)
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