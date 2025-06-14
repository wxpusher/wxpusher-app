package com.smjcco.wxpusher.login

import com.smjcco.wxpusher.base.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.IWxpBaseMvpView

interface IWxpLoginView : IWxpBaseMvpView {

    /**
     * 发送验证码按钮的文字
     */
    fun sendButtonText(msg: String, loading: Boolean)

    /**
     * 去绑定的账号
     */
    fun goBind(data: WxpLoginSendVerifyCodeResp)

    /**
     * 跳转到主页
     */
    fun goMain()
}

interface IWxpLoginPresenter : IWxpBaseMvpPresenter<IWxpLoginView> {
    /**
     * 初始化
     */
    fun init();

    /**
     * 发送验证码
     */
    suspend fun sendVerifyCode(phone: String)

    /**
     * 验证码登录
     */
    suspend fun verifyCodeLogin(phone: String, verifyCode: String): WxpLoginSendVerifyCodeResp
}