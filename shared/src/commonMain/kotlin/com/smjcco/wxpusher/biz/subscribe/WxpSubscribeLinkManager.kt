package com.smjcco.wxpusher.biz.subscribe

import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtIOSuspend
import io.ktor.http.Url
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * App 端采集的多因素，和 H5 落地页采集的口径一致：屏幕用逻辑像素（Android dp / iOS pt），竖屏短边为宽。
 */
@Serializable
data class WxpSubscribeIntentFactors(
    val os: String? = null,
    val osVersion: String? = null,
    val model: String? = null,
    val screenW: Int? = null,
    val screenH: Int? = null,
    val dpr: Double? = null,
    val netType: String? = null,
    val lang: String? = null,
    val tz: String? = null,
)

@Serializable
@JsonIgnoreUnknownKeys
data class WxpSubscribeIntentMatchResp(
    val level: String? = null,
    val intentId: String? = null,
    val type: Int? = null,
    val id: Long? = null,
    val name: String? = null,
    val extraDropped: Boolean = false,
)

/**
 * 统一订阅链接在 App 端只做路由：深链 / scheme / 开放标签拉起后，统一用 WebView 打开 H5 落地页 #/s。
 * 订阅逻辑都在 H5 和服务端，方案见 docs/subscribe-download-flow-design.md。
 */
object WxpSubscribeLinkManager {
    private const val TAG = "SubscribeLink"
    private const val SCHEME = "wxpusher"
    private const val SCHEME_HOST = "wxpusher.smjcco.com"
    private const val KEY_PENDING_URL = "subscribe_link_pending_url"
    private const val KEY_RESTORE_CHECKED = "subscribe_restore_checked"

    /** 「输入订阅暗号」页 */
    fun codeWordPageUrl(): String = "${WxpConfig.appFeUrl}/app/#/code-word"

    /** 开放标签 extinfo 只带 intentId */
    fun pageUrlForIntent(intentId: String): String =
        "${WxpConfig.appFeUrl}/app/#/s?intentId=${intentId.encodeURLParameter()}"

    /**
     * 把拉起 App 的链接转换成 H5 落地页地址，不是订阅链接时返回 null：
     * - https://{本环境域名}/api/s/{sk}?extra=&jump=（App Links / Universal Links）
     * - wxpusher://wxpusher.smjcco.com/subscribe?sk=&extra=&intentId=（落地页「打开 App 订阅」）
     */
    fun toPageUrl(link: String?): String? {
        if (link.isNullOrEmpty()) {
            return null
        }
        val url = try {
            Url(link)
        } catch (e: Throwable) {
            return null
        }
        val query = mutableListOf<Pair<String, String>>()
        val protocol = url.protocol.name.lowercase()
        if (protocol == SCHEME) {
            if (url.host != SCHEME_HOST || url.encodedPath.trimEnd('/') != "/subscribe") {
                return null
            }
            for (key in listOf("sk", "extra", "intentId")) {
                url.parameters[key]?.takeIf { it.isNotEmpty() }?.let { query.add(key to it) }
            }
        } else if (protocol == "http" || protocol == "https") {
            val apiHost = try {
                Url(WxpConfig.baseUrl).host
            } catch (e: Throwable) {
                null
            }
            val prefix = "/api/s/"
            if (url.host != apiHost || !url.encodedPath.startsWith(prefix)) {
                return null
            }
            val sk = url.encodedPath.removePrefix(prefix).trimEnd('/').decodeURLPart()
            if (sk.isEmpty() || sk.contains('/')) {
                return null
            }
            query.add("sk" to sk)
            for (key in listOf("extra", "jump")) {
                url.parameters[key]?.takeIf { it.isNotEmpty() }?.let { query.add(key to it) }
            }
        } else {
            return null
        }
        if (query.none { it.first == "sk" || it.first == "intentId" }) {
            return null
        }
        val queryString = query.joinToString("&") { "${it.first}=${it.second.encodeURLParameter()}" }
        return "${WxpConfig.appFeUrl}/app/#/s?$queryString"
    }

    /** 未登录时先记下来，登录进入首页后再打开 */
    fun savePending(pageUrl: String) {
        WxpSaveService.set(KEY_PENDING_URL, pageUrl)
    }

    fun consumePending(): String? {
        val url = WxpSaveService.get(KEY_PENDING_URL, "")
        if (url.isEmpty()) {
            return null
        }
        WxpSaveService.set(KEY_PENDING_URL, "")
        return url
    }

    /**
     * 安装后还原：首次登录进入首页时调用一次（每次安装只查一次）。
     * 匹配到高 / 中置信的订阅意图时弹窗确认，确认后按意图订阅；服务端灰度关闭时返回 none，不弹窗。
     */
    fun checkRestoreOnce(factors: WxpSubscribeIntentFactors) {
        if (WxpSaveService.get(KEY_RESTORE_CHECKED, false)) {
            return
        }
        runAtIOSuspend {
            val resp = WxpApiService.matchSubscribeIntent(factors) ?: return@runAtIOSuspend
            WxpSaveService.set(KEY_RESTORE_CHECKED, true)
            val intentId = resp.intentId
            if (intentId.isNullOrEmpty() || (resp.level != "high" && resp.level != "mid")) {
                WxpLogUtils.d(TAG, "restore: no match, level=${resp.level}")
                return@runAtIOSuspend
            }
            val name = resp.name ?: ""
            var message = "你刚才在网页上打开过它的订阅链接，现在可以一键订阅。"
            if (resp.extraDropped) {
                message += "\n\n如需关联你在「$name」的账号，订阅后请回到原来的页面，再点一次订阅链接。"
            }
            WxpDialogUtils.showDialog(
                WxpDialogParams(
                    title = "是否订阅「$name」？",
                    message = message,
                    leftText = "不是我",
                    rightText = "订阅",
                    rightBlock = { subscribeByIntent(intentId) },
                    cancelable = false,
                )
            )
        }
    }

    private fun subscribeByIntent(intentId: String) {
        runAtIOSuspend {
            val result = WxpApiService.subscribeByIntent(intentId) ?: return@runAtIOSuspend
            WxpToastUtils.showToast(if (result.code == 1) "订阅成功" else (result.msg ?: "订阅失败"))
        }
    }
}
