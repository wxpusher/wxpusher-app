package com.smjcco.wxpusher.page.messagelist

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.runAtMainSuspend

class WxpMessageListPresenter(view: IWxpMessageListView) :
    WxpBaseMvpPresenter<IWxpMessageListView, IWxpMessageListPresenter>(view),
    IWxpMessageListPresenter {
    //当前页面的列表的数据
    private var messageListData: MutableList<WxpMessageListMessage> = mutableListOf()

    //前页面的最后一条消息id
    private var lastUserReceiveRecordId = Long.MAX_VALUE
    private var key: String? = null
    private var hasMore: Boolean = true

    override fun searchIfChanged(key: String?) {
        if (this.key != key) {
            this.key = key
            refresh()
        }
    }

    override fun refresh() {
        runAtMainSuspend {
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
            }
        }
    }

    override fun loadMore() {
        runAtMainSuspend {
            view?.showMessageMoreLoading(true)
            val req = WxpMessageListReq(lastUserReceiveRecordId, key)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            view?.showMessageMoreLoading(false)
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
        }
    }
}