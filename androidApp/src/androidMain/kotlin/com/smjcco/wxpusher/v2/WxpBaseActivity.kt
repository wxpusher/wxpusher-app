package com.smjcco.wxpusher.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.smjcco.wxpusher.login.IWxpLoginView
import com.smjcco.wxpusher.login.WxpLoginSendVerifyCodeResp

abstract class WxpBaseActivity<P> : ComponentActivity() {
    protected val presenter: P = createPresenter()

    abstract fun createPresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}