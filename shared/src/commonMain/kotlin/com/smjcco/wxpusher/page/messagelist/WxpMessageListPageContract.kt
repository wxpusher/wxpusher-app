package com.smjcco.wxpusher.page.messagelist

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

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

    /**
     * 打开订阅管理页面
     */
    fun onOpenSubscribeManagerPage(url: String);

    /**
     * 当检查到不可接受消息原因的时候调用
     */
    fun onCheckReason(data: WxpCheckAppMsgReasonResp?);

    /**
     * 当检查到新公告的时候调用
     */
    fun onListBanner(data: WxpListBannerResp?);

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
     * 自检查收不到消息的原因
     * 内部带有10分钟截流限制，避免频繁访问服务器
     */
    fun fetchCheckReason()

    /**
     * 获取公告内容
     * 内部带有10分钟截流限制，避免频繁访问服务器
     */
    fun fetchListBanner()

    /**
     * 关闭banner，不再显示
     */
    fun closeListBanner(bannerId: Int?)

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

    /**
     * 批量标记消息已读状态
     * @param ids 要操作的消息 id 集合，调用方需保证长度 <= 200
     * @param read 是否标记为已读状态
     */
    fun markMessageReadStatusBatch(ids: List<Long>, read: Boolean)

    /**
     * 批量删除消息（内部会弹二次确认对话框）
     * @param ids 要删除的消息 id 集合，调用方需保证长度 <= 200
     * @param onConfirmed 用户在确认弹窗点击了"删除"之后、实际请求发出之前触发，视图可借此退出多选态
     */
    fun deleteByIds(ids: List<Long>, onConfirmed: (() -> Unit)? = null)

    /**
     * 删除当前用户的全部消息（内部会弹二次确认对话框）
     */
    fun deleteAll()

    /**
     * 打开订阅管理页面
     * 如果本地有openid，直接打开 ，如果没有，就请求一次云端，获取openid
     */
    fun openSubscribeManagerPage();
}