package com.smjcco.wxpusher.page.changephone

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

interface IWxpChangePhoneView : IWxpBaseMvpView<IWxpChangePhonePresenter> {
    /**
     * 发送验证码按钮的文字
     */
    fun onSendButtonText(msg: String)

    /**
     * 换绑手机号完成
     */
    fun onChangPhoneFinish()
}

interface IWxpChangePhonePresenter :
    IWxpBaseMvpPresenter<IWxpChangePhoneView, IWxpChangePhonePresenter> {

    /**
     * 发送验证码
     */
    fun sendVerifyCode(phone: String?)

    /**
     * 验证码登录
     */
    fun bindPhone(phone: String?, verifyCode: String?)

}