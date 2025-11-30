package com.smjcco.wxpusher.page.registerorbind

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView
import com.smjcco.wxpusher.page.login.WxpBindPageData

interface IWxpRegisterOrBindView : IWxpBaseMvpView<IWxpRegisterOrBindPresenter> {

    /**
     * 跳转到主页
     */
    fun onGoMain()

}

interface IWxpRegisterOrBindPresenter :
    IWxpBaseMvpPresenter<IWxpRegisterOrBindView, IWxpRegisterOrBindPresenter> {

    /**
     * 绑定微信
     * @param code 微信的授权码
     * @param bindData 要绑定的的手机登录的手机  或者苹果登录数据
     */
    fun wexinBind(code: String?, bindData: WxpBindPageData?)

    /**
     * 创建一个新账号
     */
    fun createAccount(data: WxpBindPageData?)

}