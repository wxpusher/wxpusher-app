package com.smjcco.wxpusher.login

import com.smjcco.wxpusher.base.WxpBaseMvpPresenter

class WxpLoginPresenter(view: IWxpLoginView) : WxpBaseMvpPresenter<IWxpLoginView>(view),
    IWxpLoginPresenter {
    override fun init() {
    }

    override suspend fun sendVerifyCode(phone: String) {
    }

    override suspend fun verifyCodeLogin(
        phone: String,
        verifyCode: String
    ): WxpLoginSendVerifyCodeResp {
        TODO("Not yet implemented")
    }

}