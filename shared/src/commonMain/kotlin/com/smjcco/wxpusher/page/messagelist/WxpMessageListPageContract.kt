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


    /**
     * 在刷新，加载更多的时候，给予用户反馈
     * iOS上是震动一下
     */
    fun onFeedback();

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
     * @param manual是否手动触发的
     */
    fun refresh(scene: Int)

    /**
     * 加载更多数据
     */
    fun loadMore()

    /**
     * 返回app的时候，可以静默获取一次消息
     * 主要用于iOS，应用未杀死，收到消息的时候app没有运行，返回前台，收到的消息不显示
     * 这个时候调用此接口，获取一次最新的消息，静默插入列表
     */
    fun fetchMessageResume()

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