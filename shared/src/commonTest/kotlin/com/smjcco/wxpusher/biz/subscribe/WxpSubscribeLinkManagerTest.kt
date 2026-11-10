package com.smjcco.wxpusher.biz.subscribe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 拉起 App 的链接转换成 H5 落地页地址（默认生产域名配置）
 */
class WxpSubscribeLinkManagerTest {

    private val page = "https://wxpusher.zjiecode.com/app/#/subscribe-link?"

    @Test
    fun httpsLink_withExtraAndJump() {
        assertEquals(
            page + "sk=K7M3QX&extra=order%201&jump=auto",
            WxpSubscribeLinkManager.toPageUrl("https://wxpusher.zjiecode.com/api/s/K7M3QX?extra=order%201&jump=auto"),
        )
        assertEquals(page + "sk=K7M3QX", WxpSubscribeLinkManager.toPageUrl("https://wxpusher.zjiecode.com/api/s/K7M3QX/"))
    }

    @Test
    fun httpsLink_chineseWordIsDecodedThenEncodedOnce() {
        assertEquals(
            page + "sk=%E5%A4%A9%E6%B0%94%E9%A2%84%E6%8A%A5",
            WxpSubscribeLinkManager.toPageUrl("https://wxpusher.zjiecode.com/api/s/%E5%A4%A9%E6%B0%94%E9%A2%84%E6%8A%A5"),
        )
    }

    @Test
    fun schemeLink_keepsSkExtraIntentId() {
        assertEquals(
            page + "sk=K7M3QX&extra=a%26b&intentId=I1",
            WxpSubscribeLinkManager.toPageUrl("wxpusher://wxpusher.smjcco.com/subscribe?sk=K7M3QX&extra=a%26b&intentId=I1&other=x"),
        )
        assertEquals(page + "intentId=I2", WxpSubscribeLinkManager.toPageUrl("wxpusher://wxpusher.smjcco.com/subscribe?intentId=I2"))
    }

    @Test
    fun notSubscribeLink_returnsNull() {
        assertNull(WxpSubscribeLinkManager.toPageUrl(null))
        assertNull(WxpSubscribeLinkManager.toPageUrl(""))
        //别的域名、别的路径、多级路径
        assertNull(WxpSubscribeLinkManager.toPageUrl("https://example.com/api/s/K7M3QX"))
        assertNull(WxpSubscribeLinkManager.toPageUrl("https://wxpusher.zjiecode.com/api/message/1"))
        assertNull(WxpSubscribeLinkManager.toPageUrl("https://wxpusher.zjiecode.com/api/s/"))
        assertNull(WxpSubscribeLinkManager.toPageUrl("https://wxpusher.zjiecode.com/api/s/a/b"))
        //消息详情 scheme、缺少 sk 和 intentId 的订阅 scheme、别的 scheme
        assertNull(WxpSubscribeLinkManager.toPageUrl("wxpusher://wxpusher.smjcco.com/message?id=1"))
        assertNull(WxpSubscribeLinkManager.toPageUrl("wxpusher://wxpusher.smjcco.com/subscribe?extra=x"))
        assertNull(WxpSubscribeLinkManager.toPageUrl("weixin://wxpusher.smjcco.com/subscribe?sk=K7M3QX"))
    }

    @Test
    fun intentPageUrl() {
        assertEquals(page + "intentId=I%2F3", WxpSubscribeLinkManager.pageUrlForIntent("I/3"))
    }
}
