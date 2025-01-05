package com.smjcco.wxpusher

import android.app.Application
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils

class WxPusherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.application = this
        SaveUtils.init()
    }
}