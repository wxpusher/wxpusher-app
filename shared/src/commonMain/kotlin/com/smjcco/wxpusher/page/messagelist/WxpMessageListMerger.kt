package com.smjcco.wxpusher.page.messagelist

/**
 * 增量拉取的合并结果
 * @param list 合并后的列表，按 messageId 降序
 * @param replaced 是否判定为断层，整个列表被第一页替换了
 * @param syncedMaxId 合并后新的同步锚点
 */
data class WxpMessageListMergeResult(
    val list: List<WxpMessageListMessage>,
    val replaced: Boolean,
    val syncedMaxId: Long,
)

/**
 * 消息列表的合并规则，纯函数，方便单元测试。
 */
object WxpMessageListMerger {

    //后端一页固定返回30条（UserReceiveRecordMapper.findUserMessageHistoryList 的 LIMIT 0,30）
    const val SERVER_PAGE_SIZE = 30

    /**
     * 增量拉取后，把服务端第一页合并进本地列表
     *
     * 同步锚点 syncedMaxId 是最近一次从服务端拉到的最大 id，WS 插入的消息不会推进它。
     * 不能直接用本地最大 id 判断：WS 离线缓存最多只重放10条，重放的新消息会让本地最大 id 变大，
     * 但它前面的消息并不全，用本地最大 id 会把断层误判成有重叠。
     *
     * @param local 本地当前列表，按 messageId 降序
     * @param serverPage 服务端返回的第一页，按 messageId 降序
     * @param syncedMaxId 同步锚点，0 表示还没有同步过
     */
    fun merge(
        local: List<WxpMessageListMessage>,
        serverPage: List<WxpMessageListMessage>,
        syncedMaxId: Long,
    ): WxpMessageListMergeResult {
        val serverMaxId = serverPage.maxOfOrNull { it.messageId } ?: syncedMaxId
        //本地为空，直接用服务端数据
        if (local.isEmpty()) {
            return WxpMessageListMergeResult(serverPage.toList(), true, serverMaxId)
        }
        //服务端为空，可能是暂时的，不能因此清空本地列表
        if (serverPage.isEmpty()) {
            return WxpMessageListMergeResult(local.toList(), false, syncedMaxId)
        }
        //拉满一页，并且最小的 id 都比锚点大，说明锚点之后的新消息超过一页，中间有丢失，直接替换
        val serverMinId = serverPage.minOf { it.messageId }
        if (serverPage.size >= SERVER_PAGE_SIZE && serverMinId > syncedMaxId) {
            return WxpMessageListMergeResult(serverPage.toList(), true, serverMaxId)
        }
        //有重叠：本地没有的按 id 补进来，本地已有的只把未读同步成已读，不反向改
        //（列表里点开消息时本地先标记已读，服务端要等详情页加载后才标记，反向同步会把刚看过的消息改回未读）
        val serverById = serverPage.associateBy { it.messageId }
        val merged = local.map { localMessage ->
            val serverMessage = serverById[localMessage.messageId]
            if (serverMessage != null && serverMessage.read && !localMessage.read) {
                localMessage.copy(read = true)
            } else {
                localMessage
            }
        }.toMutableList()
        val localIds = local.mapTo(HashSet()) { it.messageId }
        serverPage.filterTo(merged) { it.messageId !in localIds }
        merged.sortByDescending { it.messageId }
        return WxpMessageListMergeResult(merged, false, maxOf(syncedMaxId, serverMaxId))
    }

    /**
     * 把最近通过推送插入、但被服务端结果覆盖掉的消息补回去
     * 只补比列表最新一条还新的消息：刷新请求发出后才入库的消息，服务端结果里不会有
     */
    fun restoreRecentPushed(
        list: List<WxpMessageListMessage>,
        recentPushed: List<WxpMessageListMessage>,
    ): List<WxpMessageListMessage> {
        val listMaxId = list.maxOfOrNull { it.messageId } ?: 0L
        val listIds = list.mapTo(HashSet()) { it.messageId }
        val missing = recentPushed.filter { it.messageId > listMaxId && it.messageId !in listIds }
        if (missing.isEmpty()) {
            return list
        }
        return (list + missing.distinctBy { it.messageId }).sortedByDescending { it.messageId }
    }

    /**
     * 按 messageId 降序插入一条消息，已存在同 id 的消息时返回 null
     */
    fun insert(
        list: List<WxpMessageListMessage>,
        message: WxpMessageListMessage,
    ): List<WxpMessageListMessage>? {
        if (list.any { it.messageId == message.messageId }) {
            return null
        }
        val result = list.toMutableList()
        val insertIndex = result.indexOfFirst { it.messageId < message.messageId }
        if (insertIndex == -1) {
            result.add(message)
        } else {
            result.add(insertIndex, message)
        }
        return result
    }
}
