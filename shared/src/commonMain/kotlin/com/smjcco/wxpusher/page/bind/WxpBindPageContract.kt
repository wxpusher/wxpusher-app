package com.smjcco.wxpusher.page.bind

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

interface IWxpBindView : IWxpBaseMvpView<IWxpBindPresenter> {

    /**
     * 跳转到主页
     */
    fun onGoMain()
}

interface IWxpBindPresenter : IWxpBaseMvpPresenter<IWxpBindView, IWxpBindPresenter> {

    /**
     * 查询绑定状态
     */
    fun queryBindStatus(phone: String?, verifyCode: String?)

}