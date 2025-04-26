package com.smjcco.wxpusher.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.notification.NotificationManager.sendBizMessageNotification
import com.smjcco.wxpusher.ws.IWsMessageListener
import com.smjcco.wxpusher.ws.InitDeviceMsg
import com.smjcco.wxpusher.ws.PushMsgDeviceMsg
import com.smjcco.wxpusher.ws.WsManager
import com.smjcco.wxpusher.ws.WsMessageTypeEnum
import com.xiaomi.mipush.sdk.MiPushClient

object ApplicationUtils {
    lateinit var application: Application
    private val TAG = "AppInit"

    /**
     * 判断是不是主进程
     */
    fun isMainProcess(): Boolean {
        val am = (application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        val processInfos = am.runningAppProcesses
        val mainProcessName: String = application.getPackageName()
        val myPid = Process.myPid()
        for (info in processInfos) {
            if (info.pid == myPid && mainProcessName == info.processName) {
                return true
            }
        }
        return false
    }

    /**
     * 注册推送通道
     */
    fun regPushChannel() {
        if (AppDataUtils.getLoginInfo()?.deviceToken?.isNotEmpty() != true) {
            Log.d(TAG, "initPushChannel: 没有登陆，不初始化通道")
            return
        }
        if (DeviceUtils.isMIUI()) {
            regMiPush()
        } else {
            initSelfWsBiz()
        }
    }

    /**
     * 反注册推送通道
     */
    fun unRegPushChannel() {
        if (DeviceUtils.isMIUI()) {
            MiPushClient.unregisterPush(application)
        } else {
            WsManager.disconnect()
        }
    }

    private fun initSelfWsBiz() {
        //当收到消息的时候，发送到通知栏
        val pushListener = object : IWsMessageListener<PushMsgDeviceMsg> {
            override fun onMessage(message: PushMsgDeviceMsg) {
                sendBizMessageNotification(message)
            }
        }
        WsManager.addMsgListener(WsMessageTypeEnum.PUSH_NOTE.code, pushListener)

        val pushTokenListener = object : IWsMessageListener<InitDeviceMsg> {
            override fun onMessage(message: InitDeviceMsg) {
                Log.d(TAG, "收到自建长链接Ws的pushToken=${message.pushToken}")
                if (AppDataUtils.getPushToken() == message.pushToken) {
                    Log.d(TAG, "自建长链接token未变化，不用更新 pushToken=${message.pushToken}")
                    return
                }
                AppDataUtils.savePushToken(message.pushToken)
                DeviceApi.updateDeviceInfoAsync()
            }
        }
        WsManager.addMsgListener(WsMessageTypeEnum.DEVICE_INIT.code, pushTokenListener)
        WsManager.init()
    }

    /**
     * 初始化小米推送
     */
    private fun regMiPush() {
        Log.d(TAG, "初始化小米推送")
        MiPushClient.registerPush(application, "2882303761520373007", "5932037320007")
    }
}