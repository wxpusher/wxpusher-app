package com.smjcco.wxpusher.page.accountdetail

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView
import com.smjcco.wxpusher.page.login.WxpBindPageData

interface IWxpAccountDetailView : IWxpBaseMvpView<IWxpAccountDetailPresenter> {
    fun onWeixinBindSuccess()

    fun onAppleBindSuccess()
}

interface IWxpAccountDetailPresenter :
    IWxpBaseMvpPresenter<IWxpAccountDetailView, IWxpAccountDetailPresenter> {

    /**
     * 微信登录
     * @param code 微信的授权码
     */
    fun weixinBind(code: String?)

    /**
     * 苹果登录
     * @param code 苹果登录返回的jwt
     * @param userId 用户id
     * @param email 电子邮箱，可能是苹果提供的隐私邮箱
     * @param name 苹果的用户名，只有第一次授权会有
     */
    fun appleBind(code: String?, userId: String?, email: String?, name: String?)

    /**
     * 退出登录
     */
    fun logout()

}