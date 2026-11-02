package com.smjcco.wxpusher.push

import android.app.Activity
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.vivo.VivoSubscribeManager
import com.smjcco.wxpusher.push.xiaomi.XiaomiSubscribeManager
import com.smjcco.wxpusher.utils.PermissionUtils
import java.time.OffsetDateTime

/** 厂商订阅通道的授权状态，上报在 WxpVendorState.subscribe，跟服务端 VendorSubscribeState 一一对应。 */
enum class VendorSubscribeState {
    /** 已授权，通道是开的。 */
    GRANTED,

    /** 授权过，后来用户在系统设置里关掉了。服务端不再发厂商推送。 */
    DISABLED,

    /** 从来没授权过。 */
    DENIED,

    /** 设备不支持订阅功能。 */
    UNSUPPORTED,
}

/**
 * 厂商订阅通道的授权引导（小米、vivo）。
 *
 * 两家的新规都是：订阅消息必须走订阅通道 + 模板，用户在系统弹窗里授权后才会展示，老发法在切换日关停。
 * 授权状态一律以本地通知渠道的实际开关为准，通过 [PushChannelCoordinator] 跟 platform / token 一起上报，
 * 服务端按状态选通道。方案见 monorepo docs/xiaomi-push-subscribe-template-design.md、
 * docs/vivo-push-subscribe-template-design.md。
 */
abstract class VendorSubscribeGuide {
    companion object {
        /** 切换日前多少天起，每次冷启动都提醒一次。 */
        private const val REMIND_EVERY_LAUNCH_BEFORE_CUTOFF_MILLIS = 7 * 24 * 3600_000L

        /** 这个厂商有没有订阅通道授权，没有返回 null。 */
        fun of(platform: DevicePlatform?): VendorSubscribeGuide? = when (platform) {
            DevicePlatform.Android_XIAOMI -> XiaomiSubscribeManager
            DevicePlatform.Android_VIVO -> VivoSubscribeManager
            else -> null
        }

        /** 当前设备厂商的授权引导。 */
        fun current(): VendorSubscribeGuide? = of(PushChannelCoordinator.getSnapshot().vendorPlatform)
    }

    /** 对应的厂商平台。 */
    protected abstract val platform: DevicePlatform

    /** 设置页那一行的标题，例如「小米订阅通知授权」。 */
    abstract val settingTitle: String

    /** 升级后是否已经自动提醒过一次的存储 key。 */
    protected abstract val autoPromptedKey: String

    /** 远程配置里的引导开关，出问题时可以远程关掉。 */
    protected abstract fun isGuideEnabled(): Boolean

    /** 远程配置里的切换日，格式错误时用 [defaultLegacyCutoff]。 */
    protected abstract fun legacyCutoffConfig(): String

    protected abstract val defaultLegacyCutoff: String

    /** 读取当前设备的授权状态，只读本地状态，可以在主线程频繁调用。 */
    abstract fun currentState(): VendorSubscribeState

    /** 消息列表顶部 banner 的提示文案。 */
    abstract fun getBannerText(): String

    /** 先弹我们自己的说明，再拉起系统授权弹窗或者跳系统设置。 */
    abstract fun showGuide(activity: Activity)

    /** 打开订阅通道的系统设置页。 */
    abstract fun openChannelSettings(activity: Activity)

    private var autoPromptedThisProcess = false

    /** 老发法是否已经关停，切换日从远程配置读。 */
    fun isLegacyCutoffPassed(): Boolean = System.currentTimeMillis() >= getLegacyCutoffTime()

    protected fun getLegacyCutoffTime(): Long {
        return runCatching { OffsetDateTime.parse(legacyCutoffConfig()).toInstant().toEpochMilli() }
            .getOrElse { OffsetDateTime.parse(defaultLegacyCutoff).toInstant().toEpochMilli() }
    }

    /**
     * 是否需要引导用户开启：当前厂商、当前在用厂商推送、已登录、应用通知已开、
     * 远程配置打开，并且状态是没授权或者被关掉了。
     */
    fun needGuide(activity: Activity?): Boolean {
        if (PushChannelCoordinator.getSnapshot().vendorPlatform != platform) {
            return false
        }
        if (PushPlatformState.getEffectivePushChannel() != PushChannel.VENDOR) {
            return false
        }
        if (WxpAppDataService.getLoginInfo()?.deviceId.isNullOrEmpty()) {
            return false
        }
        if (!PermissionUtils.hasNotificationPermission(activity)) {
            return false
        }
        if (!isGuideEnabled()) {
            return false
        }
        val state = currentState()
        return state == VendorSubscribeState.DENIED || state == VendorSubscribeState.DISABLED
    }

    /**
     * 首页自动提醒：升级后第一次进首页提醒一次；切换日前 7 天起每次冷启动都提醒。
     */
    fun autoPromptIfNeeded(activity: Activity) {
        if (autoPromptedThisProcess || !needGuide(activity)) {
            return
        }
        val nearCutoff = System.currentTimeMillis() >=
            getLegacyCutoffTime() - REMIND_EVERY_LAUNCH_BEFORE_CUTOFF_MILLIS
        if (WxpSaveService.get(autoPromptedKey, false) && !nearCutoff) {
            return
        }
        autoPromptedThisProcess = true
        WxpSaveService.set(autoPromptedKey, true)
        showGuide(activity)
    }
}
