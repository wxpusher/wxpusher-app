package com.smjcco.wxpusher

import android.app.Application
import com.smjcco.wxpusher.utils.ApplicationUtils

class WxPusherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.application = this
    }
}