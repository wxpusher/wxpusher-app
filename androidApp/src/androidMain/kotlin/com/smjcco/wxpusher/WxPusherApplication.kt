package com.smjcco.wxpusher

import android.app.Application
import android.util.Log
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.ws.KeepWsConnectService
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.notification.NotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.smjcco.wxpusher.ws.IWsMessageListener
import com.smjcco.wxpusher.ws.InitDeviceMsg
import com.smjcco.wxpusher.ws.PushMsgDeviceMsg
import com.smjcco.wxpusher.ws.WsManager
import com.smjcco.wxpusher.ws.WsMessageTypeEnum

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
}