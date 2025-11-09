package com.smjcco.wxpusher.page.login

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

interface IWxpLoginView : IWxpBaseMvpView<IWxpLoginPresenter> {

    /**
     * 发送验证码按钮的文字
     */
    fun onSendButtonText(msg: String, loading: Boolean)

    /**
     * 去绑定的账号
     */
    fun onGoBind(phone: String, code: String, data: WxpLoginSendVerifyCodeResp)

    /**
     * 跳转到主页
     */
    fun onGoMain()
}

interface IWxpLoginPresenter : IWxpBaseMvpPresenter<IWxpLoginView, IWxpLoginPresenter> {
    /**
     * 初始化
     */
    fun init();

    /**
     * 发送验证码
     */
    fun sendVerifyCode(phone: String?)

    /**
     * 验证码登录
     */
    fun verifyCodeLogin(phone: String?, verifyCode: String?)

    /**
     * 微信登录
     * @param code 微信的授权码
     */
    fun wexinLogin(code: String?)
}