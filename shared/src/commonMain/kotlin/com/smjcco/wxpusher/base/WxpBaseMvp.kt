package com.smjcco.wxpusher.base


interface IWxpBaseMvpView

interface IWxpBaseMvpPresenter<V : IWxpBaseMvpView> {
    fun onShow()
    fun onDestroy()
}

abstract class WxpBaseMvpPresenter<V : IWxpBaseMvpView>(var view: V?) : IWxpBaseMvpPresenter<V> {
    override fun onShow() {

    }

    override fun onDestroy() {
        this.view = null
    }
}