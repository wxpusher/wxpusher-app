package com.smjcco.wxpusher.page.messagelist

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WxpMessageListPresenter(view: IWxpMessageListView) :
    WxpBaseMvpPresenter<IWxpMessageListView, IWxpMessageListPresenter>(view),
    IWxpMessageListPresenter {

    //冷启动的时候被点击的消息
    private var clickMessage: WxpMessageListMessage? = null

    //当前页面的列表的数据
    private var messageListData: MutableList<WxpMessageListMessage> = mutableListOf()

    private val MessageRefreshTimeKey = "WxpMessageList_MessageRefreshTimeKey"
    private val BannerHasCloseIdKey = "WxpMessageList_hasCloseBannerId"

    //前页面的最后一条消息id
    private var lastMessageId = Long.MAX_VALUE

    //至少>20条才需要加载更多，后端目前下发的是一页30条 ，避免每次加载首页，都多请求一次
    private var pageMinCount = 20
    private var key: String? = null
    private var hasMore: Boolean = true

    private var loading: Boolean = false

    //上次消息自建的时间
    private var lastCheckNoMsgTime: Long = 0
    private var checkNoMsgResp: WxpCheckAppMsgReasonResp? = null

    //上次获取公告的时间
    private var lastFetchNoTime: Long = 0

    //同步锚点：最近一次从服务端拉到的最大消息 id，推送插入的消息不推进它，规则见 WxpMessageListMerger.merge
    private var syncedMaxId: Long = 0

    //请求进行中又需要增量拉取，先记下来，当前请求结束后补拉一次
    private var pendingFetch = false

    //请求进行中用户触发了刷新，先记下场景，当前请求结束后再刷新，避免刷新被丢掉、下拉头一直转
    private var pendingRefreshScene: Int? = null

    //当前是否有全量刷新在跑，以及它用的搜索词：同一个搜索词的刷新再来一次时直接忽略
    private var refreshInFlight = false
    private var refreshingKey: String? = null

    //厂商到达信号的防抖任务
    private var signalDebounceJob: Job? = null
    private val SignalDebounceMillis = 1500L

    //回前台增量拉取的节流
    private var lastForegroundFetchTime: Long = 0
    private val ForegroundThrottleMillis = 5000L

    //最近通过推送插入的消息（插入时间, 消息），刷新结果把它们覆盖掉时补回来
    private val recentPushedMessages = ArrayDeque<Pair<Long, WxpMessageListMessage>>()
    private val RecentPushedKeepMillis = 60_000L
    private val RecentPushedMaxSize = 50

    override fun searchIfChanged(key: String?) {
        if (this.key != key) {
            this.key = key
            refresh(WxpMessageListReq.SceneSearch)
        }
    }

    override fun init() {
        WxpAppDataService.getCacheMessageList()?.let {
            messageListData = it.toMutableList()
            //缓存本身就是服务端数据，可以作为同步锚点
            syncedMaxId = it.maxOfOrNull { message -> message.messageId } ?: 0
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
        //搜索中不往搜索结果里插入，退出搜索时会全量刷新
        if (!key.isNullOrEmpty()) {
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

    override fun onPushMessageArrived(message: WxpMessageListMessage) {
        if (isNotLogin()) {
            return
        }
        //搜索中不往搜索结果里插入，退出搜索时会全量刷新
        if (!key.isNullOrEmpty()) {
            WxpLogUtils.d(message = "推送消息到达-搜索中，不插入，messageId=${message.messageId}")
            return
        }
        //WS 重放、切换通道期间两条通道都送达，都会走到这里，按 id 去重
        val newList = WxpMessageListMerger.insert(messageListData, message) ?: return
        WxpLogUtils.d(message = "推送消息到达-插入列表，messageId=${message.messageId}")
        messageListData = newList.toMutableList()
        recordRecentPushed(message)
        view?.onMessageList(messageListData.toList())
        saveRefreshListData()
    }

    override fun onPushSignal() {
        signalDebounceJob?.cancel()
        signalDebounceJob = WxpScopeUtils.getMainScope().launch {
            delay(SignalDebounceMillis)
            WxpLogUtils.d(message = "推送到达信号-增量拉取")
            fetchIncremental()
        }
    }

    override fun onAppForeground() {
        val now = WxpDateTimeUtils.getTimestamp()
        if (now - lastForegroundFetchTime < ForegroundThrottleMillis) {
            WxpLogUtils.d(message = "回到前台-5秒内已拉取过，跳过")
            return
        }
        lastForegroundFetchTime = now
        WxpLogUtils.d(message = "回到前台-增量拉取")
        fetchIncremental()
    }

    override fun onDestroy() {
        signalDebounceJob?.cancel()
        super.onDestroy()
    }

    override fun refresh(scene: Int) {
        WxpLogUtils.d(message = "开始刷新-refresh")
        WxpLogUtils.d(message = "clickMessage=${clickMessage}")

        if (loading) {
            //正在跑的就是同一个搜索词的全量刷新，结果一样，不用再刷一次，它结束时会收起下拉动画
            //（鸿蒙列表页冷启动时会连着触发两次刷新，不忽略就会多发一次请求）
            if (refreshInFlight && refreshingKey == key) {
                return
            }
            //当前有其他请求在跑，等它结束后再刷新
            pendingRefreshScene = scene
            return
        }
        //手动触发的，就给予震动反馈
        if (WxpMessageListReq.SceneManual == scene) {
            view?.onFeedback()
        }
        runAtMainSuspend {
            loading = true
            refreshInFlight = true
            refreshingKey = key
            view?.showMessageRefreshing(true)
            val req = WxpMessageListReq(Long.MAX_VALUE, key, scene)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            view?.showMessageRefreshing(false)
            WxpSaveService.set(MessageRefreshTimeKey, WxpDateTimeUtils.getTimestamp().toDouble())
            //如果刷新的数据不为null，说明是刷新成功了,然后才更新数据
            if (fetchResultList != null) {
                //如果有被点击消息，就更新一下被点击消息的状态
                //只用来改成已读：被点击消息可能是推送到达时记下的未读状态，不能用它把服务端的已读改回未读
                if (clickMessage?.read == true) {
                    fetchResultList.find { it.messageId == clickMessage?.messageId }?.read = true
                    WxpLogUtils.d(message = "标记消息已读=${clickMessage}")
                }
                var resultList: List<WxpMessageListMessage> = fetchResultList
                //搜索结果不能作为同步锚点，也不补回推送消息
                if (key.isNullOrEmpty()) {
                    syncedMaxId = fetchResultList.maxOfOrNull { it.messageId } ?: 0
                    resultList = WxpMessageListMerger.restoreRecentPushed(
                        resultList,
                        getRecentPushed()
                    )
                }
                messageListData = resultList.toMutableList()
                lastMessageId = messageListData.lastOrNull()?.messageId ?: Long.MAX_VALUE
                hasMore = messageListData.size >= pageMinCount
                view?.showMessageMoreLoading(false, hasMore)
                view?.onMessageList(messageListData.toList())
                saveRefreshListData()
            }
            loading = false
            refreshInFlight = false
            runPendingRequest()
        }
    }

    override fun fetchMessageResume() {
        WxpLogUtils.d(message = "开始刷新-fetchMessageResume")
        fetchIncremental()
    }

    /**
     * 增量拉取：请求第一页，按 WxpMessageListMerger.merge 的规则合并进列表
     * 有重叠只补齐缺失的消息，有断层就用第一页替换整个列表，都只发一次请求
     */
    private fun fetchIncremental() {
        //搜索中不合并，退出搜索时会全量刷新
        if (!key.isNullOrEmpty()) {
            WxpLogUtils.d(message = "增量拉取-搜索中，跳过")
            return
        }
        if (isNotLogin()) {
            WxpLogUtils.d(message = "增量拉取-没有登录，跳过")
            return
        }
        if (loading) {
            pendingFetch = true
            return
        }
        runAtMainSuspend {
            loading = true
            val req = WxpMessageListReq(Long.MAX_VALUE, null, WxpMessageListReq.SceneFetchResume)
            val fetchResultList = WxpApiService.fetchMessageList(req)
            //请求期间进入了搜索，丢弃结果，退出搜索时会全量刷新
            if (fetchResultList != null && key.isNullOrEmpty()) {
                WxpSaveService.set(MessageRefreshTimeKey, WxpDateTimeUtils.getTimestamp().toDouble())
                val mergeResult =
                    WxpMessageListMerger.merge(messageListData, fetchResultList, syncedMaxId)
                syncedMaxId = mergeResult.syncedMaxId
                if (mergeResult.replaced) {
                    WxpLogUtils.d(message = "增量拉取-有断层，替换整个列表")
                    messageListData = WxpMessageListMerger.restoreRecentPushed(
                        mergeResult.list,
                        getRecentPushed()
                    ).toMutableList()
                    lastMessageId = messageListData.lastOrNull()?.messageId ?: Long.MAX_VALUE
                    hasMore = messageListData.size >= pageMinCount
                    view?.showMessageMoreLoading(false, hasMore)
                } else {
                    messageListData = mergeResult.list.toMutableList()
                }
                view?.onMessageList(messageListData.toList())
                saveRefreshListData()
            }
            loading = false
            runPendingRequest()
        }
    }

    /**
     * 当前请求结束后，执行期间被推迟的请求，刷新优先，刷新本身就包含了增量拉取
     */
    private fun runPendingRequest() {
        val scene = pendingRefreshScene
        if (scene != null) {
            pendingRefreshScene = null
            pendingFetch = false
            refresh(scene)
            return
        }
        if (pendingFetch) {
            pendingFetch = false
            fetchIncremental()
        }
    }

    private fun recordRecentPushed(message: WxpMessageListMessage) {
        recentPushedMessages.addLast(WxpDateTimeUtils.getTimestamp() to message)
        while (recentPushedMessages.size > RecentPushedMaxSize) {
            recentPushedMessages.removeFirst()
        }
    }

    /**
     * 获取最近 60 秒内通过推送插入的消息，顺便清理过期的
     */
    private fun getRecentPushed(): List<WxpMessageListMessage> {
        val expireTime = WxpDateTimeUtils.getTimestamp() - RecentPushedKeepMillis
        while (recentPushedMessages.isNotEmpty() && recentPushedMessages.first().first < expireTime) {
            recentPushedMessages.removeFirst()
        }
        return recentPushedMessages.map { it.second }
    }

    private fun isNotLogin(): Boolean {
        return WxpAppDataService.getLoginInfo()?.deviceToken.isNullOrEmpty()
    }

    override fun getTipsOfLastRefreshTime(): String {
        val time = WxpSaveService.get(MessageRefreshTimeKey, 0.0)
        if (time <= 0.0) {
            return "更新于 无"
        }
        return "更新于 " + WxpDateTimeUtils.getRelativeDateTime(time.toLong())
    }

    /**
     * 保存一下第一页的数据，方便下次启动的时候用缓存渲染
     */
    private fun saveRefreshListData() {
        //搜索的时候，不保存缓存，否则下次启动会用搜索结果渲染首页
        if (!key.isNullOrEmpty()) {
            return
        }
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
        //增量拉取和加载更多都会改列表，不能同时跑，结束加载状态让用户稍后再拉
        if (loading) {
            WxpLogUtils.d(message = "loadMore-有请求进行中，不加载")
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
            runPendingRequest()
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

    override fun deleteById(id: Long) {
        val params = WxpDialogParams(
            title = "确认删除消息",
            message = "你确认删除此消息吗？删除后不可恢复。",
            leftText = "取消",
            rightText = "删除",
            rightBlock = {
                runAtMainSuspend {
                    WxpApiService.deleteMessageById(id) {
                        messageListData.removeAll { it.messageId == id }
                        view?.onMessageList(messageListData)
                    }
                }
            }
        )
        WxpDialogUtils.showDialog(params)
    }

    override fun markMessageReadStatusBatch(ids: List<Long>, read: Boolean) {
        if (ids.isEmpty()) {
            return
        }
        runAtMainSuspend {
            WxpApiService.markMessageReadStatusBatch(ids, read) {
                val idSet = ids.toHashSet()
                messageListData.forEach {
                    if (it.messageId in idSet) {
                        it.read = read
                    }
                }
                view?.onMessageList(messageListData.toList())
                saveRefreshListData()
            }
        }
    }

    override fun deleteByIds(ids: List<Long>, onConfirmed: (() -> Unit)?) {
        if (ids.isEmpty()) {
            return
        }
        val params = WxpDialogParams(
            title = "确认删除消息",
            message = "你确认删除选中的 ${ids.size} 条消息吗？删除后不可恢复。",
            leftText = "取消",
            rightText = "删除",
            rightBlock = {
                // 用户点击了确认再退出多选态，保证用户在确认弹窗上仍能看到已选条目数
                onConfirmed?.invoke()
                runAtMainSuspend {
                    WxpApiService.deleteMessagesByIds(ids) {
                        val idSet = ids.toHashSet()
                        messageListData.removeAll { it.messageId in idSet }
                        view?.onMessageList(messageListData.toList())
                        saveRefreshListData()
                    }
                }
            }
        )
        WxpDialogUtils.showDialog(params)
    }

    override fun deleteAll() {
        val params = WxpDialogParams(
            title = "确认删除全部消息",
            message = "此操作会删除你的全部消息，删除后不可恢复，确认继续吗？",
            leftText = "取消",
            rightText = "全部删除",
            rightBlock = {
                runAtMainSuspend {
                    WxpApiService.deleteAllMessages {
                        messageListData.clear()
                        view?.onMessageList(emptyList())
                        saveRefreshListData()
                    }
                }
            }
        )
        WxpDialogUtils.showDialog(params)
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
            view?.onOpenSubscribeManagerPage("${WxpConfig.appFeUrl}/app/#/subscribe-list")
        }
    }

    override fun fetchCheckReason() {
        //超过10分钟才进行一次检测，避免请求后端太频繁
        if (WxpDateTimeUtils.getTimestamp() - lastCheckNoMsgTime < 10 * 60 * 1000) {
            view?.onCheckReason(checkNoMsgResp)
            return
        }
        runAtMainSuspend {
            checkNoMsgResp = WxpApiService.checkReason()
            lastCheckNoMsgTime = WxpDateTimeUtils.getTimestamp()
            view?.onCheckReason(checkNoMsgResp)
        }
    }

    override fun fetchListBanner() {
        //超过10分钟才进行一次检测，避免请求后端太频繁
        if (WxpDateTimeUtils.getTimestamp() - lastFetchNoTime < 10 * 60 * 1000) {
            return
        }
        runAtMainSuspend {
            val listBannerResp = WxpApiService.getListBanner()
            lastFetchNoTime = WxpDateTimeUtils.getTimestamp()
            if (listBannerResp?.id == WxpSaveService.get(BannerHasCloseIdKey, 0)) {
                return@runAtMainSuspend
            }
            view?.onListBanner(listBannerResp)
        }
    }

    override fun closeListBanner(bannerId: Int?) {
        bannerId?.let { WxpSaveService.set(BannerHasCloseIdKey, it) }
        view?.onListBanner(null)
    }


}