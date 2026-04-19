package com.smjcco.wxpusher.biz.version

/**
 * 跳转应用市场的能力抽象，由两端各自注入实现。
 * Android：按 DeviceUtils.getPlatform() 分流到厂商应用商店；downgradeToTbs=true 或厂商失败 → TBS。
 * iOS：忽略 downgradeToTbs，直接打开 downloadUrl（通常是 itms-apps 链接）。
 */
interface WxpAppMarketNavigator {
    fun jumpToMarket(downloadUrl: String, downgradeToTbs: Boolean)

    /**
     * 本次跳转通道是否自带升级弹窗（例如 Android 的 TBS）。
     * 返回 true 时，shared 层跳过自己的"发现新版本"弹窗，直接触发 [jumpToMarket]，
     * 避免出现 shared 弹窗 + 通道弹窗 两次点击。
     * iOS 永远返回 false（App Store 打开后没有应用内弹窗）。
     */
    fun willShowInternalDialog(downgradeToTbs: Boolean): Boolean
}
