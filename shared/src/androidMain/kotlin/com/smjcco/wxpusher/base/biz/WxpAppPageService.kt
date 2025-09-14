package com.smjcco.wxpusher.base.biz

import android.content.Intent
import com.smjcco.wxpusher.base.common.ApplicationUtils

actual fun WxpAppPageService_jumpToLogin() {
    val application = ApplicationUtils.getApplication()
    application.startActivity(Intent())
}

