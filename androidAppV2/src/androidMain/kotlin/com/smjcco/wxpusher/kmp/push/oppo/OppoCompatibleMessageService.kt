package com.smjcco.wxpusher.kmp.push.oppo

import android.content.Context
import com.heytap.msp.push.mode.DataMessage
import com.heytap.msp.push.service.CompatibleDataMessageCallbackService
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.utils.GsonUtils

class OppoCompatibleMessageService : CompatibleDataMessageCallbackService() {

    override fun processMessage(p0: Context?, p1: DataMessage?) {
        super.processMessage(p0, p1)
        WxPusherLog.i("OPPO", GsonUtils.toJson(p1))
    }

}