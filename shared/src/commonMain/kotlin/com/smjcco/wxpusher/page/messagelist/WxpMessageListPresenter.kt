package com.smjcco.wxpusher.page.messagelist

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.base.biz.WxpAppDataService

class WxpMessageListPresenter(view: IWxpMessageListView) :
    WxpBaseMvpPresenter<IWxpMessageListView, IWxpMessageListPresenter>(view),
    IWxpMessageListPresenter {

    //冷启动的时候被点击的消息
    private var clickMessage: WxpMessageListMessage? = null

    //当前页面的列表的数据
    private var messageListData: MutableList<WxpMessageListMessage> = mutableListOf()

    private val MessageRefreshTimeKey = "WxpMessageList_MessageRefreshTimeKey"


    //前页面的最后一条消息id
    private var lastMessageId = Long.MAX_VALUE

    //至少>20条才需要加载更多，后端目前下发的是一页30条 ，避免每次加载首页，都多请求一次
    private var pageMinCount = 20
    private var key: String? = null
    private var hasMore: Boolean = true

    private var loading: Boolean = false

    override fun searchIfChanged(key: String?) {
        if (this.key != key) {
            this.key = key
            refresh(WxpMessageListReq.SceneSearch)
        }
    }

    override fun init() {
        WxpAppDataService.getCacheMessageList()?.let {
            messageListData = it.toMutableList()
            view?.onMessageList(it)
        }
    }

    override fun onReceiveNewMessage(message: WxpMessageListMessage) {
        //点击消息冷启动的时候，可能消息还没有拉回来，缓存一下被点击的消息，拉回来的时候，检查是同一条消息，就标记为已读。
        clickMessage = message;
        //如果更新的消息已经存在，就更新阅读状态
        messageListData.find { it.messageId == message.messageId }?.let {
            it.read = message.read;
            // 通知视图更新
            view?.onMessageList(messageListData.toList())
            saveRefreshListData()
            return
        }
        // 找到第一个 id 小于新消息 id 的位置
        val insertIndex = messageListData.indexOfFirst { it.messageId < message.messageId }
        if (insertIndex == -1) {
            // 如果没有找到更小的 id，说明新消息的 id 是最小的，添加到列表末尾
            messageListData.add(message)
        } else {
            // 在找到的位置插入新消息
            messageListData.add(insertIndex, message)
        }
        // 通知视图更新
        view?.onMessageList(messageListData.toList())
        saveRefreshListData()
    }

    override fun refresh(scene: Int) {
        WxpLogUtils.d(message = "开始刷新-refresh")
        WxpLogUtils.d(message = "clickMessage=${clickMessage}")

        if (loading) {
            return
        }
        //手动触发的，就给予震动反馈
        if (WxpMessageListReq.SceneManual == scene) {
            view?.onFeedback()
        }
        runAtMainSuspend {
            loading = true
            view?.showMessageRefreshing(true)
            val req = WxpMessageListReq(Long.MAX_VALUE, key, scene)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            view?.showMessageRefreshing(false)
            WxpSaveService.set(MessageRefreshTimeKey, WxpDateTimeUtils.getTimestamp().toDouble())
            //如果刷新的数据不为null，说明是刷新成功了,然后才更新数据
            if (fetchResultList != null) {
                //如果有被点击消息，就更新一下被点击消息的状态
                if (clickMessage != null) {
                    fetchResultList.find { it.messageId == clickMessage?.messageId }?.read =
                        (clickMessage?.read == true)
                    WxpLogUtils.d(message = "标记消息已读=${clickMessage}")
                }
                messageListData = fetchResultList.toMutableList()
                lastMessageId = messageListData.lastOrNull()?.messageId ?: Long.MAX_VALUE
                hasMore = messageListData.size >= pageMinCount
                view?.showMessageMoreLoading(false, hasMore)
                view?.onMessageList(messageListData.toList())
                //搜索的时候，不保存缓存
                if (key.isNullOrEmpty()) {
                    saveRefreshListData()
                }
            }
            loading = false
        }
    }

    override fun fetchMessageResume() {
        WxpLogUtils.d(message = "开始刷新-fetchMessageResume")
        if (loading) {
            return
        }
        if (WxpAppDataService.getLoginInfo()?.deviceToken.isNullOrEmpty()) {
            WxpLogUtils.d(message = "开始刷新-fetchMessageResume-没有登录，放弃刷新")
            return
        }
        runAtMainSuspend {
            loading = true
            val req = WxpMessageListReq(Long.MAX_VALUE, key, WxpMessageListReq.SceneFetchResume)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            WxpSaveService.set(MessageRefreshTimeKey, WxpDateTimeUtils.getTimestamp().toDouble())
            if (fetchResultList != null) {
                for (message in fetchResultList) {
                    var has = false
                    for (listMessage in messageListData) {
                        if (listMessage.messageId == message.messageId) {
                            has = true
                            break
                        }
                    }
                    if (has) {
                        continue
                    }
                    // 找到第一个 id 小于新消息 id 的位置
                    val insertIndex =
                        messageListData.indexOfFirst { it.messageId < message.messageId }
                    if (insertIndex == -1) {
                        // 如果没有找到更小的 id，说明新消息的 id 是最小的，添加到列表末尾
                        messageListData.add(message)
                    } else {
                        // 在找到的位置插入新消息
                        messageListData.add(insertIndex, message)
                    }
                }
                view?.onMessageList(messageListData.toList())
                saveRefreshListData()
            }
            loading = false
        }
    }

    override fun getTipsOfLastRefreshTime(): String {
        val time = WxpSaveService.get(MessageRefreshTimeKey, 0.0)
        if (time <= 0.0) {
            return "更新于 无"
        }
        return "更新于 " + WxpDateTimeUtils.getRelativeDateTime(time)
    }

    /**
     * 保存一下第一页的数据，方便下次启动的时候用缓存渲染
     */
    private fun saveRefreshListData() {
        WxpAppDataService.setCacheMessageList(messageListData)
    }


    override fun loadMore() {
        if (!hasMore) {
            WxpLogUtils.d(message = "loadMore-没有更多数据，不进行加载")
            return
        }
        if (messageListData.size < pageMinCount) {
            WxpLogUtils.d(message = "loadMore-数据不够1页，不加载更多")
            hasMore = false
            view?.showMessageMoreLoading(false, hasMore)
            return
        }
        //加载更多，给予一个震动反馈
        view?.onFeedback()
        runAtMainSuspend {
            loading = true
            view?.showMessageMoreLoading(true, hasMore)
            val req = WxpMessageListReq(lastMessageId, key, WxpMessageListReq.SceneLoadMore)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            if (fetchResultList !== null) {
                if (fetchResultList.isEmpty()) {
                    //为空，说明没有更多数据了
                    hasMore = false
                } else {
                    messageListData.addAll(fetchResultList)
                    lastMessageId =
                        messageListData.lastOrNull()?.messageId ?: Long.MAX_VALUE
                    view?.onMessageList(messageListData.toList())
                }
            }
            view?.showMessageMoreLoading(false, hasMore)
            loading = false
        }
    }

    override fun markMessageReadStatus(messageId: Long?, read: Boolean) {
        runAtMainSuspend {
            WxpApiService.markMessageReadStatus(messageId, read) {
                if (messageId == null) {
                    messageListData.forEach {
                        it.read = read
                    }
                } else {
                    messageListData.find { it.messageId == messageId }?.read = read
                }

                view?.onMessageList(messageListData)
            }
        }
    }

    override fun deleteById(messageId: Long) {
        runAtMainSuspend {
            WxpApiService.deleteMessageById(messageId) {
                messageListData.removeAll { it.messageId == messageId }
                view?.onMessageList(messageListData)
            }
        }
    }

    override fun openSubscribeManagerPage() {
        runAtMainSuspend {
            var openId = WxpAppDataService.getLoginInfo()?.openId
            if (openId.isNullOrEmpty()) {
                //如果为空，从网络请求一次openid
                openId = WxpApiService.getOpenId()
                if (openId == null || openId.isEmpty()) {
                    return@runAtMainSuspend
                }
                WxpAppDataService.saveOpenId(openId)
            }
            view?.onOpenSubscribeManagerPage("${WxpConfig.baseUrl}/wxuser/?openId=${openId}#/")
        }
    }
}