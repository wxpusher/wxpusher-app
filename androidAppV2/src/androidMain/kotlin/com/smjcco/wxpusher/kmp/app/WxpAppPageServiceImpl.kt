package com.smjcco.wxpusher.kmp.app

import android.content.Intent
import com.smjcco.wxpusher.base.biz.IWxpAppPageService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.kmp.page.login.WxpLoginActivity

class WxpAppPageServiceImpl : IWxpAppPageService {

    override fun jumpToLogin() {
        val application = ApplicationUtils.getApplication()
        val intent = Intent(application, WxpLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        application.startActivity(intent)
    }
}