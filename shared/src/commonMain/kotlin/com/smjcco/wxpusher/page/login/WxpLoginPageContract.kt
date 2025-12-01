package com.smjcco.wxpusher.page.login

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

interface IWxpLoginView : IWxpBaseMvpView<IWxpLoginPresenter> {

    /**
     * 发送验证码按钮的文字
     */
    fun onSendButtonText(msg: String, loading: Boolean)

    /**
     * 去选择注册方式：微信登录绑定、公众号绑定、直接创建新账号
     */
    fun onGoBindOrCreateAccount(data: WxpBindPageData)

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
    fun weixinLogin(code: String?)

    /**
     * 苹果登录
     * @param code 苹果登录返回的jwt
     * @param userId 用户id
     * @param email 电子邮箱，可能是苹果提供的隐私邮箱
     * @param name 苹果的用户名，只有第一次授权会有
     */
    fun appleLogin(code: String?, userId: String?, email: String?, name: String?)
}