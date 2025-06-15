package com.smjcco.wxpusher.page.messagelist

import com.smjcco.wxpusher.base.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.IWxpBaseMvpView

interface IWxpMessageListView : IWxpBaseMvpView<IWxpMessageListPresenter> {

    /**
     * 列表页面是否在刷新中
     */
    fun showMessageRefreshing(refreshing: Boolean)

    /**
     * 列表页面是否在加载中
     */
    fun showMessageMoreLoading(loading: Boolean)

    /**
     * 列表页面的数据
     */
    fun onMessageList(data: List<WxpMessageListMessage>)


}

interface IWxpMessageListPresenter :
    IWxpBaseMvpPresenter<IWxpMessageListView, IWxpMessageListPresenter> {
    /**
     * 刷新数据
     */
    fun refresh(key: String?)

    /**
     * 加载更多数据
     */
    fun loadMore()
}