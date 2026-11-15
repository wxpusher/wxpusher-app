package com.smjcco.wxpusher

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Tests initialize only WS dependencies, never production SDKs or app startup requests. */
class WsTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, WsTestApplication::class.java.name, context)
}

class WsTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.smjcco.wxpusher.base.common.WxpLogUtils.setEnabled(false)
        com.smjcco.wxpusher.base.common.ApplicationUtils.init(this)
        com.smjcco.wxpusher.base.common.WxpSaveService.init()
        com.smjcco.wxpusher.push.PushChannelStore.setWsRequested(false)
    }
}
