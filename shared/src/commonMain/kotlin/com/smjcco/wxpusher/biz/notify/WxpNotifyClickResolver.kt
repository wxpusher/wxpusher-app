package com.smjcco.wxpusher.biz.notify

import com.smjcco.wxpusher.base.common.WxpSaveService

/**
 * 点击通知后要打开的地址。
 */
data class WxpNotifyClickTarget(
    val url: String,
    // true：交给系统打开（可能拉起外部 App）；false：App 内 WebView 打开
    val openExternal: Boolean,
    // 系统打开失败时改为打开的地址（详情页），App 内打开时为 null
    val fallbackUrl: String?
)

/**
 * 「点击通知直接打开原文链接」：决定点击通知后打开详情页还是原文链接。
 * 与鸿蒙 shared/biz/notify/WxpNotifyClickResolver.ts 规则逐条一致，改动时两边同步。
 */
object WxpNotifyClickResolver {
    // 与 H5（app-fe common/NotifyClickSetting.ts）约定的存储 key，值为 "1" 表示开启
    const val SETTING_KEY = "notify_click_open_source_url"

    private val webSchemes = setOf("http", "https")

    // 会执行脚本、读取本地文件、拉起 App 内部页面，或者绕回自己的 deeplink，一律不打开
    private val blockedSchemes = setOf(
        "javascript", "vbscript", "data", "file", "content", "blob", "about",
        "intent", "android-app", "wxpusher"
    )

    private val schemeRegex = Regex("^[A-Za-z][A-Za-z0-9+.\\-]*$")

    fun isEnabled(): Boolean = WxpSaveService.get(SETTING_KEY, "") == "1"

    /**
     * @param detailUrl 消息详情页地址
     * @param sourceUrl 开发者传入的原文链接
     * @return 要打开的地址；detailUrl 和原文链接都用不了时返回 null，保持现有逻辑不跳转
     */
    fun resolve(detailUrl: String?, sourceUrl: String?): WxpNotifyClickTarget? {
        val detail = detailUrl?.trim().orEmpty()
        val detailTarget = if (detail.isEmpty()) null else WxpNotifyClickTarget(detail, false, null)
        if (!isEnabled()) {
            return detailTarget
        }
        val source = sourceUrl?.trim().orEmpty()
        return when (classify(source)) {
            SourceUrlType.WEB -> WxpNotifyClickTarget(source, false, null)
            SourceUrlType.EXTERNAL -> WxpNotifyClickTarget(source, true, detailTarget?.url)
            SourceUrlType.INVALID -> detailTarget
        }
    }

    private enum class SourceUrlType { WEB, EXTERNAL, INVALID }

    private fun classify(url: String): SourceUrlType {
        if (url.isEmpty()) {
            return SourceUrlType.INVALID
        }
        // 浏览器会忽略 scheme 里的 Tab、换行，java\tscript: 这类写法靠这一步挡住
        if (url.any { it.code < 0x20 || it.code == 0x7F }) {
            return SourceUrlType.INVALID
        }
        val colonIndex = url.indexOf(':')
        if (colonIndex <= 0) {
            return SourceUrlType.INVALID
        }
        val rawScheme = url.substring(0, colonIndex)
        if (!schemeRegex.matches(rawScheme)) {
            return SourceUrlType.INVALID
        }
        val scheme = rawScheme.lowercase()
        if (scheme in webSchemes) {
            val rest = url.substring(colonIndex + 1)
            if (!rest.startsWith("//")) {
                return SourceUrlType.INVALID
            }
            val host = rest.substring(2).takeWhile { it != '/' && it != '?' && it != '#' }
            return if (host.isEmpty()) SourceUrlType.INVALID else SourceUrlType.WEB
        }
        if (scheme in blockedSchemes) {
            return SourceUrlType.INVALID
        }
        return SourceUrlType.EXTERNAL
    }
}
