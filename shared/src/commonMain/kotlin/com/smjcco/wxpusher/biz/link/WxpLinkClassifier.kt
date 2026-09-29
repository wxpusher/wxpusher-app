package com.smjcco.wxpusher.biz.link

/**
 * 链接的打开方式
 */
enum class WxpLinkType {
    // http/https 网页，App 内 WebView 打开
    WEB,

    // 其他 App 的链接（weixin://、alipays://、tel: 等），交给系统打开，可能拉起外部 App
    EXTERNAL,

    // 空的、格式不对，或者危险的链接，不打开
    INVALID
}

/**
 * 按 scheme 判断链接的打开方式，各端 jumpToWebUrl 统一用它决定怎么打开。
 * 与鸿蒙 shared/biz/link/WxpLinkClassifier.ts 规则逐条一致，改动时两边同步。
 */
object WxpLinkClassifier {

    private val webSchemes = setOf("http", "https")

    // 会执行脚本、读取本地文件、拉起 App 内部页面，或者绕回自己的 deeplink，一律不打开
    private val blockedSchemes = setOf(
        "javascript", "vbscript", "data", "file", "content", "blob", "about",
        "intent", "android-app", "wxpusher"
    )

    private val schemeRegex = Regex("^[A-Za-z][A-Za-z0-9+.\\-]*$")

    fun classify(url: String?): WxpLinkType {
        val link = url?.trim().orEmpty()
        if (link.isEmpty()) {
            return WxpLinkType.INVALID
        }
        // 浏览器会忽略 scheme 里的 Tab、换行，java\tscript: 这类写法靠这一步挡住
        if (link.any { it.code < 0x20 || it.code == 0x7F }) {
            return WxpLinkType.INVALID
        }
        val colonIndex = link.indexOf(':')
        if (colonIndex <= 0) {
            return WxpLinkType.INVALID
        }
        val rawScheme = link.substring(0, colonIndex)
        if (!schemeRegex.matches(rawScheme)) {
            return WxpLinkType.INVALID
        }
        val scheme = rawScheme.lowercase()
        if (scheme in webSchemes) {
            val rest = link.substring(colonIndex + 1)
            if (!rest.startsWith("//")) {
                return WxpLinkType.INVALID
            }
            val host = rest.substring(2).takeWhile { it != '/' && it != '?' && it != '#' }
            return if (host.isEmpty()) WxpLinkType.INVALID else WxpLinkType.WEB
        }
        if (scheme in blockedSchemes) {
            return WxpLinkType.INVALID
        }
        return WxpLinkType.EXTERNAL
    }
}
