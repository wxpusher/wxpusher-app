package com.smjcco.wxpusher.base


interface IWxpBaseMvpView<P> {
    fun createPresenter(): P
}

interface IWxpBaseMvpPresenter<V : IWxpBaseMvpView<P>, P> {
    fun onShow()
    fun onDestroy()
}

abstract class WxpBaseMvpPresenter<V : IWxpBaseMvpView<P>, P>(var view: V?) :
    IWxpBaseMvpPresenter<V, P> {
    override fun onShow() {

    }

    override fun onDestroy() {
        this.view = null
    }
}