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
    fun showMessageMoreLoading(loading: Boolean, hasMore: Boolean)

    /**
     * 列表页面的数据
     */
    fun onMessageList(data: List<WxpMessageListMessage>)


}

interface IWxpMessageListPresenter :
    IWxpBaseMvpPresenter<IWxpMessageListView, IWxpMessageListPresenter> {

    /**
     * 初始化，使用本地数据渲染，在数据没有回来之前，把消息列表页面渲染出来
     * 内部不进行刷新，需要单独调用刷新
     */
    fun init()

    /**
     * 获取上次刷新时间
     */
    fun getTipsOfLastRefreshTime(): String

    /**
     * 当收到新的消息，把数据插入到了列表里面
     */
    fun onReceiveNewMessage(message: WxpMessageListMessage);

    /**
     * 如果关键字改变， 就调用这个方法，如果关键字改变了，就会刷新搜索
     */
    fun searchIfChanged(key: String?)

    /**
     * 刷新数据
     */
    fun refresh()

    /**
     * 加载更多数据
     */
    fun loadMore()

    /**
     * 标记消息已读状态
     * @param id 消息的消息id，不传表示标记用户的所有消息
     * @param read 是否标记为已读状态
     */
    fun markMessageReadStatus(
        id: Long? = null,
        read: Boolean
    )

    /**
     * 删除消息
     * @param id 消息的消息id
     */
    fun deleteById(id: Long)
}