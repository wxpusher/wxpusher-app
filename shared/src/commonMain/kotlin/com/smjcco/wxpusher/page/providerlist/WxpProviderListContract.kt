package com.smjcco.wxpusher.page.providerlist

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

interface IWxpProviderListView : IWxpBaseMvpView<IWxpProviderListPresenter> {
    fun onLoadPage(url: String);
}

interface IWxpProviderListPresenter :
    IWxpBaseMvpPresenter<IWxpProviderListView, IWxpProviderListPresenter> {
    fun loadPage();
}