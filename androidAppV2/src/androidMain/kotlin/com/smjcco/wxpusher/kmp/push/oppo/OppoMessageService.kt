package com.smjcco.wxpusher.kmp.push.oppo

import android.content.Context
import com.heytap.msp.push.mode.DataMessage
import com.heytap.msp.push.service.DataMessageCallbackService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.kmp.common.utils.GsonUtils

class OppoMessageService : DataMessageCallbackService() {
    override fun processMessage(p0: Context?, p1: DataMessage?) {
        super.processMessage(p0, p1)
        WxpLogUtils.i("OPPO2", GsonUtils.toJson(p1))
    }
}