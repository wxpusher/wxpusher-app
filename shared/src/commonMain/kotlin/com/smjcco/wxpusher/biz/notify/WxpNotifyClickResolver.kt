package com.smjcco.wxpusher.biz.notify

import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.biz.link.WxpLinkClassifier
import com.smjcco.wxpusher.biz.link.WxpLinkType

/**
 * 点击通知后要打开的地址，交给 jumpToWebUrl 按 scheme 决定 App 内打开还是交给系统打开。
 */
data class WxpNotifyClickTarget(
    val url: String,
    // 交给系统打开失败（没有能打开的 App）时改为打开的地址，即详情页
    val fallbackUrl: String?
)

/**
 * 「点击通知直接打开原文链接」：决定点击通知后打开详情页还是原文链接。
 * 与鸿蒙 shared/biz/notify/WxpNotifyClickResolver.ts 逻辑一致，改动时两边同步。
 */
object WxpNotifyClickResolver {
    // 与 H5（app-fe common/NotifyClickSetting.ts）约定的存储 key，值为 "1" 表示开启
    const val SETTING_KEY = "notify_click_open_source_url"

    fun isEnabled(): Boolean = WxpSaveService.get(SETTING_KEY, "") == "1"

    /**
     * @param detailUrl 消息详情页地址
     * @param sourceUrl 开发者传入的原文链接
     * @return 要打开的地址；详情页地址为空时返回 null，保持现有逻辑不跳转
     */
    fun resolve(detailUrl: String?, sourceUrl: String?): WxpNotifyClickTarget? {
        val detail = detailUrl?.trim().orEmpty()
        if (detail.isEmpty()) {
            return null
        }
        if (!isEnabled()) {
            return WxpNotifyClickTarget(detail, null)
        }
        val source = sourceUrl?.trim().orEmpty()
        // 没有原文链接，或者原文链接不能打开（危险、格式不对），都打开详情页
        if (WxpLinkClassifier.classify(source) == WxpLinkType.INVALID) {
            return WxpNotifyClickTarget(detail, null)
        }
        return WxpNotifyClickTarget(source, detail)
    }
}
