package com.smjcco.wxpusher.v2

import android.os.Bundle
import com.smjcco.wxpusher.page.login.IWxpLoginPresenter
import com.smjcco.wxpusher.page.login.IWxpLoginView
import com.smjcco.wxpusher.page.login.WxpLoginPresenter
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeResp

class LoginActivity : WxpBaseMvpActivity<IWxpLoginPresenter>(), IWxpLoginView {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.init()
    }

    override fun createPresenter(): IWxpLoginPresenter {
        return WxpLoginPresenter(this)
    }

    override fun onSendButtonText(msg: String, loading: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onGoBind(phone: String, code: String, data: WxpLoginSendVerifyCodeResp) {
        TODO("Not yet implemented")
    }

    override fun onGoMain() {
        TODO("Not yet implemented")
    }

}