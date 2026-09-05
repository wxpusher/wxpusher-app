package com.smjcco.wxpusher.push

import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.bean.DevicePlatform

/** 用户主动选择的推送通道。系统强制降级不会覆盖该选择。 */
enum class PushChannelPreference {
    VENDOR,
    WEBSOCKET,
}

/** 当前实际生效的推送通道，用于页面展示和运行时判断。 */
enum class PushChannel {
    VENDOR,
    WEBSOCKET,
}

/**
 * 启用 WebSocket 通道的原因。
 *
 * 原因会被持久化，以便应用重启后继续执行正确的恢复策略。
 */
enum class WsActivationReason {
    /** 用户在设置页主动选择。 */
    USER_SELECTED,

    /** 首次获取厂商 token 失败后的临时降级。 */
    INITIAL_VENDOR_FALLBACK,

    /** ConfigManager 全局开关关闭厂商通道后的强制降级。 */
    SYSTEM_CONFIG_FORCED,

    /** 当前设备没有可用的厂商推送能力。 */
    UNSUPPORTED_DEVICE,
}

/** 厂商推送在设置页中的可用状态。 */
enum class VendorAvailability {
    READY,
    REGISTERING,
    UNSUPPORTED,
    REGISTER_FAILED,
    CONFIG_DISABLED,
}

/** 持久化的推送路由目标，平台与 token 必须成对读取和写入。 */
internal data class PushChannelTarget(
    val platform: DevicePlatform,
    val token: String,
)

/** Android 推送通道状态持久化。 */
object PushChannelStore {
    // V2 不再猜测历史厂商 token 的来源，避免把 HMS token 误标成荣耀等其他平台。
    private const val KEY_MIGRATED_V2 = "PushChannel_MigratedV2"

    // 用户选择与厂商注册状态。
    private const val KEY_PREFERENCE = "PushChannel_UserPreference"
    private const val KEY_VENDOR_REGISTERED = "PushChannel_VendorEverRegistered"
    private const val KEY_VENDOR_PLATFORM = "PushChannel_VendorPlatform"
    private const val KEY_VENDOR_TOKEN = "PushChannel_VendorToken"

    // WebSocket 与当前实际生效通道的状态。
    private const val KEY_WS_TOKEN = "PushChannel_WsToken"
    private const val KEY_EFFECTIVE_PLATFORM = "PushChannel_EffectivePlatform"
    private const val KEY_EFFECTIVE_TOKEN = "PushChannel_EffectiveToken"
    private const val KEY_WS_REASON = "PushChannel_WsReason"
    private const val KEY_WS_REQUESTED = "PushChannel_WsRequested"

    /**
     * 首次升级时迁移旧版 token。
     *
     * `PT_` 前缀可以明确识别为 WebSocket token；历史厂商 token 没有保存来源平台，
     * 因此不能根据当前手机品牌猜测其平台，只能清理新结构并等待厂商 SDK 返回新 token。
     */
    fun migrateIfNeeded() {
        if (WxpSaveService.get(KEY_MIGRATED_V2, false)) {
            return
        }

        val currentToken = WxpAppDataService.getPushToken().orEmpty()
        if (currentToken.startsWith("PT_")) {
            setWsToken(currentToken)
            saveEffectivePushTarget(DevicePlatform.Android, currentToken)
            if (
                getPreference() == PushChannelPreference.VENDOR
                && getWsReason() == null
            ) {
                setWsReason(WsActivationReason.INITIAL_VENDOR_FALLBACK)
            }
        } else {
            // 历史厂商 token 来源不可信，保留服务端旧路由，等待本机 SDK 重新注册后再覆盖。
            clearVendorRegistration()
            clearEffectivePushTarget()
            setVendorEverRegistered(false)
            WxpAppDataService.savePushToken("")
        }
        WxpSaveService.set(KEY_MIGRATED_V2, true)
    }

    /** 获取用户主动选择的推送通道。 */
    fun getPreference(): PushChannelPreference = runCatching {
        PushChannelPreference.valueOf(
            WxpSaveService.get(KEY_PREFERENCE, PushChannelPreference.VENDOR.name)
        )
    }.getOrDefault(PushChannelPreference.VENDOR)

    /** 保存用户主动选择的推送通道，系统强制降级不能覆盖该值。 */
    fun setPreference(preference: PushChannelPreference) {
        WxpSaveService.set(KEY_PREFERENCE, preference.name)
    }

    /** 判断当前设备是否曾成功取得过合法厂商 token。 */
    fun hasVendorEverRegistered(): Boolean =
        WxpSaveService.get(KEY_VENDOR_REGISTERED, false)

    /** 保存当前设备是否曾成功取得过合法厂商 token。 */
    fun setVendorEverRegistered(value: Boolean) {
        WxpSaveService.set(KEY_VENDOR_REGISTERED, value)
    }

    /** 保存厂商推送注册成功后返回的平台和 token。 */
    internal fun saveVendorRegistration(platform: DevicePlatform, token: String) {
        WxpSaveService.set(KEY_VENDOR_PLATFORM, platform.getPlatform())
        WxpSaveService.set(KEY_VENDOR_TOKEN, token)
    }

    /** 获取已缓存且平台信息完整的厂商推送目标。 */
    internal fun getVendorPushTarget(): PushChannelTarget? {
        val platform = DevicePlatform.find(WxpSaveService.get(KEY_VENDOR_PLATFORM, ""))
            ?: return null
        val token = WxpSaveService.get(KEY_VENDOR_TOKEN, "")
        if (token.isEmpty()) {
            return null
        }
        return PushChannelTarget(platform, token)
    }

    /** 获取已缓存的厂商推送 token。 */
    fun getVendorToken(): String = WxpSaveService.get(KEY_VENDOR_TOKEN, "")

    /** 清理无法确认来源或已经失效的厂商注册信息。 */
    internal fun clearVendorRegistration() {
        WxpSaveService.set(KEY_VENDOR_PLATFORM, "")
        WxpSaveService.set(KEY_VENDOR_TOKEN, "")
    }

    /** 保存 WebSocket 服务返回的 token。 */
    fun setWsToken(token: String) {
        WxpSaveService.set(KEY_WS_TOKEN, token)
    }

    /** 获取最近一次 WebSocket 服务返回的 token。 */
    fun getWsToken(): String = WxpSaveService.get(KEY_WS_TOKEN, "")

    /** 成对保存当前已经与后端同步成功的推送平台和 token。 */
    internal fun saveEffectivePushTarget(platform: DevicePlatform, token: String) {
        WxpSaveService.set(KEY_EFFECTIVE_PLATFORM, platform.getPlatform())
        WxpSaveService.set(KEY_EFFECTIVE_TOKEN, token)
    }

    /** 清理无法确认平台与 token 对应关系的历史生效目标。 */
    internal fun clearEffectivePushTarget() {
        WxpSaveService.set(KEY_EFFECTIVE_PLATFORM, "")
        WxpSaveService.set(KEY_EFFECTIVE_TOKEN, "")
    }

    /**
     * 成对读取当前已经与后端同步成功的推送平台和 token。
     *
     * 平台字段为空或无法识别时返回空；token 是否有效由状态层和协调器按具体场景判断。
     */
    internal fun getEffectivePushTarget(): PushChannelTarget? {
        val platform = DevicePlatform.find(WxpSaveService.get(KEY_EFFECTIVE_PLATFORM, ""))
            ?: return null
        val token = WxpSaveService.get(KEY_EFFECTIVE_TOKEN, "")
        return PushChannelTarget(platform, token)
    }

    /** 保存当前使用 WS 的原因；传空表示已经不再处于 WS 特殊状态。 */
    fun setWsReason(reason: WsActivationReason?) {
        WxpSaveService.set(KEY_WS_REASON, reason?.name ?: "")
    }

    /** 获取当前使用 WS 的原因。 */
    fun getWsReason(): WsActivationReason? = runCatching {
        WsActivationReason.valueOf(WxpSaveService.get(KEY_WS_REASON, ""))
    }.getOrNull()

    /** 保存当前是否需要维持 WS 连接和前台保活服务。 */
    fun setWsRequested(requested: Boolean) {
        WxpSaveService.set(KEY_WS_REQUESTED, requested)
    }

    /** 判断当前是否需要维持 WS 连接和前台保活服务。 */
    fun isWsRequested(): Boolean = WxpSaveService.get(KEY_WS_REQUESTED, false)

}
