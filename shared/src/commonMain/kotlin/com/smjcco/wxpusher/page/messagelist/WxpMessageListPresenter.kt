package com.smjcco.wxpusher.page.messagelist

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.WxpDateTimeUtils
import com.smjcco.wxpusher.base.WxpSaveService
import com.smjcco.wxpusher.base.WxpToastUtils
import com.smjcco.wxpusher.base.letOnNotEmpty
import com.smjcco.wxpusher.base.runAtMainSuspend
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class WxpMessageListPresenter(view: IWxpMessageListView) :
    WxpBaseMvpPresenter<IWxpMessageListView, IWxpMessageListPresenter>(view),
    IWxpMessageListPresenter {
    //当前页面的列表的数据
    private var messageListData: MutableList<WxpMessageListMessage> = mutableListOf()

    private val SaveCacheKey = "WxpMessageList_MessageSaveCacheKey"
    private val MessageRefreshTimeKey = "WxpMessageList_MessageRefreshTimeKey"


    //前页面的最后一条消息id
    private var lastUserReceiveRecordId = Long.MAX_VALUE
    private var key: String? = null
    private var hasMore: Boolean = true

    private var loading: Boolean = false

    override fun searchIfChanged(key: String?) {
        if (this.key != key) {
            this.key = key
            refresh()
        }
    }

    override fun init() {
        val messageDataStr = WxpSaveService.get(SaveCacheKey, "")
        messageDataStr.letOnNotEmpty {
            messageListData = Json.decodeFromString(it)
            view?.onMessageList(messageListData.toList())
        }
    }

    override fun onReceiveNewMessage(message: WxpMessageListMessage) {
        // 找到第一个 id 小于新消息 id 的位置
        val insertIndex = messageListData.indexOfFirst { it.id < message.id }
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

    override fun refresh() {
        if (loading) {
            return
        }
        runAtMainSuspend {
            loading = true
            view?.showMessageRefreshing(true)
            val req = WxpMessageListReq(Long.MAX_VALUE, key)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            view?.showMessageRefreshing(false)
            //如果刷新的数据不为null，说明是刷新成功了,然后才更新数据
            if (fetchResultList != null) {
                messageListData = fetchResultList.toMutableList()
                lastUserReceiveRecordId = messageListData.lastOrNull()?.id ?: Long.MAX_VALUE
                hasMore = true
                view?.onMessageList(messageListData.toList())
                saveRefreshListData()
            }
            loading = false
        }
    }

    @OptIn(ExperimentalTime::class)
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
    @OptIn(ExperimentalTime::class)
    private fun saveRefreshListData() {
        val dataStr = Json.encodeToString(messageListData)
        WxpSaveService.set(SaveCacheKey, dataStr)
        WxpSaveService.set(
            MessageRefreshTimeKey,
            Clock.System.now().toEpochMilliseconds().toString()
        )
    }


    override fun loadMore() {
        if (!hasMore) {
            println("没有更多数据了")
            return
        }
        runAtMainSuspend {
            loading = true
            view?.showMessageMoreLoading(true, hasMore)
            val req = WxpMessageListReq(lastUserReceiveRecordId, key)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            if (fetchResultList !== null) {
                if (fetchResultList.isEmpty()) {
                    //为空，说明没有更多数据了
                    hasMore = false
                } else {
                    messageListData.addAll(fetchResultList)
                    lastUserReceiveRecordId = messageListData.lastOrNull()?.id ?: Long.MAX_VALUE
                    view?.onMessageList(messageListData.toList())
                }
            }
            view?.showMessageMoreLoading(false, hasMore)
            loading = false
        }
    }

    override fun markMessageReadStatus(id: Long?, read: Boolean) {
        runAtMainSuspend {
            WxpApiService.markMessageReadStatus(id, read) {
                if(id==null){
                    messageListData.forEach {
                        it.read = read
                    }
                }else{
                    messageListData.find { it.id == id }?.read = read
                }

                view?.onMessageList(messageListData)
            }
        }
    }
}