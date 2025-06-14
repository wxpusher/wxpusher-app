package com.smjcco.wxpusher.v2

import android.os.Bundle
import com.smjcco.wxpusher.login.IWxpLoginPresenter
import com.smjcco.wxpusher.login.IWxpLoginView
import com.smjcco.wxpusher.login.WxpLoginPresenter
import com.smjcco.wxpusher.login.WxpLoginSendVerifyCodeResp

class LoginActivity : WxpBaseActivity<IWxpLoginPresenter>(), IWxpLoginView {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.init()
    }

    override fun createPresenter(): IWxpLoginPresenter {
        return WxpLoginPresenter(this)
    }

    override fun sendButtonText(msg: String, loading: Boolean) {
        TODO("Not yet implemented")
    }

    override fun goBind(data: WxpLoginSendVerifyCodeResp) {
        TODO("Not yet implemented")
    }

    override fun goMain() {
        TODO("Not yet implemented")
    }
}