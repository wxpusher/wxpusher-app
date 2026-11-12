package com.smjcco.wxpusher.page.messagelist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WxpMessageListMergerTest {

    private fun msg(id: Long, read: Boolean = false) = WxpMessageListMessage(
        messageId = id,
        url = "https://example.com/$id",
        sourceUrl = null,
        summary = "summary-$id",
        name = "name",
        read = read,
        createTime = id,
    )

    //按 id 降序生成 [from, to] 区间的消息
    private fun range(from: Long, to: Long, read: Boolean = false) =
        (to downTo from).map { msg(it, read) }

    private fun ids(list: List<WxpMessageListMessage>) = list.map { it.messageId }

    @Test
    fun localEmpty_replacedByServerPage() {
        val result = WxpMessageListMerger.merge(emptyList(), range(1, 10), 0)
        assertTrue(result.replaced)
        assertEquals(ids(range(1, 10)), ids(result.list))
        assertEquals(10, result.syncedMaxId)
    }

    @Test
    fun serverEmpty_keepLocal() {
        val local = range(1, 10)
        val result = WxpMessageListMerger.merge(local, emptyList(), 10)
        assertFalse(result.replaced)
        assertEquals(ids(local), ids(result.list))
        assertEquals(10, result.syncedMaxId)
    }

    @Test
    fun overlap_insertNewAndKeepLoadedPages() {
        //本地已经翻页加载到 90 条（100～11），后台来了 5 条
        val local = range(11, 100)
        val result = WxpMessageListMerger.merge(local, range(76, 105), 100)
        assertFalse(result.replaced)
        assertEquals(ids(range(11, 105)), ids(result.list))
        assertEquals(105, result.syncedMaxId)
    }

    @Test
    fun lessThanOnePage_allNew_notReplaced() {
        //不足一页说明服务端已经返回全部消息，不会有丢失
        val local = range(1, 10)
        val result = WxpMessageListMerger.merge(local, range(11, 20), 10)
        assertFalse(result.replaced)
        assertEquals(ids(range(1, 20)), ids(result.list))
    }

    @Test
    fun fullPageAboveAnchor_replaced() {
        //本地 100～71，后台来了 50 条，拉到 150～121
        val local = range(71, 100)
        val result = WxpMessageListMerger.merge(local, range(121, 150), 100)
        assertTrue(result.replaced)
        assertEquals(ids(range(121, 150)), ids(result.list))
        assertEquals(150, result.syncedMaxId)
    }

    @Test
    fun fullPageMinEqualsAnchor_overlap() {
        //刚好重叠一条，补齐 29 条
        val local = range(71, 100)
        val result = WxpMessageListMerger.merge(local, range(100, 129), 100)
        assertFalse(result.replaced)
        assertEquals(ids(range(71, 129)), ids(result.list))
    }

    @Test
    fun wsReplayRaisesLocalMax_stillDetectGap() {
        //本地 100～71、锚点 100，WS 离线只重放了 131～140，回前台拉到 140～111
        val local = range(131, 140) + range(71, 100)
        val result = WxpMessageListMerger.merge(local, range(111, 140), 100)
        assertTrue(result.replaced)
        assertEquals(ids(range(111, 140)), ids(result.list))
        assertEquals(140, result.syncedMaxId)
    }

    @Test
    fun wsInsertedNewest_overlap_fillMissing() {
        //锚点 100，WS 先插入了 140，拉到 140～111 和本地 120～91 有重叠
        val local = listOf(msg(140)) + range(91, 120)
        val result = WxpMessageListMerger.merge(local, range(111, 140), 120)
        assertFalse(result.replaced)
        assertEquals(ids(range(91, 140)), ids(result.list))
        assertEquals(ids(result.list).distinct(), ids(result.list))
    }

    @Test
    fun overlap_readOnlyUpgradeToRead() {
        val local = listOf(msg(3, read = false), msg(2, read = true), msg(1, read = false))
        val server = listOf(msg(4), msg(3, read = true), msg(2, read = false))
        val result = WxpMessageListMerger.merge(local, server, 3)
        val readById = result.list.associate { it.messageId to it.read }
        //本地未读、服务端已读 → 改成已读
        assertEquals(true, readById[3])
        //本地已读、服务端未读 → 保持已读
        assertEquals(true, readById[2])
        //服务端结果里没有的本地消息不动
        assertEquals(false, readById[1])
        assertEquals(false, readById[4])
        //不修改传进来的本地对象
        assertFalse(local[0].read)
    }

    @Test
    fun anchorNeverMovesBackward() {
        val local = range(1, 10)
        val result = WxpMessageListMerger.merge(local, range(1, 5), 10)
        assertEquals(10, result.syncedMaxId)
    }

    @Test
    fun restoreRecentPushed_onlyNewerAndMissing() {
        val list = range(1, 10)
        val recent = listOf(msg(12), msg(11), msg(5), msg(12))
        val result = WxpMessageListMerger.restoreRecentPushed(list, recent)
        assertEquals(ids(range(1, 12)), ids(result))
    }

    @Test
    fun insert_dedupAndOrder() {
        val list = listOf(msg(10), msg(5))
        assertNull(WxpMessageListMerger.insert(list, msg(5)))
        assertEquals(listOf(10L, 7L, 5L), ids(WxpMessageListMerger.insert(list, msg(7))!!))
        assertEquals(listOf(11L, 10L, 5L), ids(WxpMessageListMerger.insert(list, msg(11))!!))
        assertEquals(listOf(10L, 5L, 1L), ids(WxpMessageListMerger.insert(list, msg(1))!!))
    }
}
