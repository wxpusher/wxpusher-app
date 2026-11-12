package com.smjcco.wxpusher.push

import android.app.Activity
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.biz.bean.WxpVendorState
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
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
 * 最后一次授权操作的结果，上报在 WxpVendorState.reason，跟服务端 VendorSubscribeReason 一一对应。
 * 只用来统计「为什么没授权」和排查问题，不参与路由。
 */
enum class VendorSubscribeReason {
    /** 已授权。 */
    NONE,

    /** 还没弹过我们的说明。 */
    NOT_PROMPTED,

    /** 说明弹窗点了「暂不」或者返回键关掉。 */
    GUIDE_DISMISSED,

    /** 系统弹窗里拒绝（包括没打开开关就点允许）。 */
    SYSTEM_REJECTED,

    /** vivo 拒绝次数到了上限，不会再弹窗。 */
    REJECT_THRESHOLD,

    /** 被厂商系统频控拦下。 */
    VENDOR_LIMITED,

    /** 小米获取 channel 信息失败。 */
    CHANNEL_FAILED,

    /** 不在前台，系统没弹。 */
    NOT_FOREGROUND,

    /** 应用通知权限没开。 */
    NOTIFY_OFF,

    /** 跳了系统设置页，回来还是没打开。 */
    SETTINGS_OPENED,

    /** 其他错误码，看 code。 */
    ERROR,
}

/** 说明弹窗是从哪里打开的，只用于日志。 */
enum class VendorGuideSource {
    /** 升级后或者切换日前首页自动弹出。 */
    AUTO,

    /** 消息列表顶部的提示条。 */
    BANNER,

    /** 推送通道设置页。 */
    SETTING,
}

/**
 * 厂商订阅通道的授权引导（小米、vivo）。
 *
 * 两家的新规都是：订阅消息必须走订阅通道 + 模板，用户在系统弹窗里授权后才会展示，老发法在切换日关停。
 * 授权状态一律以本地通知渠道的实际开关为准，通过 [PushChannelCoordinator] 跟 platform / token 一起上报，
 * 服务端按状态选通道。方案见 monorepo docs/xiaomi-push-subscribe-template-design.md、
 * docs/vivo-push-subscribe-template-design.md。
 *
 * 授权诊断（最后一次操作的结果、弹窗次数等）跟状态一起上报，用来统计授权漏斗；
 * 每一步都打 tag 为 [LOG_TAG] 的 key=value 日志（info 级别会上报到阿里云日志），按 did 回捞。
 */
abstract class VendorSubscribeGuide {
    companion object {
        /** 小米、vivo 授权相关的日志统一用这个 tag，方便在阿里云日志里按 tag + did 回捞。 */
        const val LOG_TAG = "VendorSubscribe"

        /** 切换日前多少天起，每次冷启动都提醒一次。 */
        private const val REMIND_EVERY_LAUNCH_BEFORE_CUTOFF_MILLIS = 7 * 24 * 3600_000L

        /** 计数的上限，跟服务端校验一致。 */
        private const val COUNT_MAX = 9999

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

    /** 日志和本地存储里的厂商名，例如 xiaomi。 */
    protected abstract val vendorName: String

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

    /** 本地订阅通道的 importance，通道不存在时返回空。 */
    protected abstract fun subscribeChannelImportance(): Int?

    /** 厂商相关的环境信息（系统是否支持、ROM、推送服务版本），只在进程里打一次日志。 */
    protected abstract fun vendorEnvInfo(): String

    /** 消息列表顶部 banner 的提示文案。 */
    abstract fun getBannerText(): String

    /** 先弹我们自己的说明，再拉起系统授权弹窗或者跳系统设置。 */
    abstract fun showGuide(activity: Activity, source: VendorGuideSource)

    /** 打开订阅通道的系统设置页。 */
    abstract fun openChannelSettings(activity: Activity)

    private var autoPromptedThisProcess = false

    /** 本进程最后一次打日志的状态 / 不弹引导的原因，相同的不重复打。 */
    private var loggedStateLine: String? = null
    private var loggedSkipReason: String? = null
    private var envLogged = false
    private var requestStartAt = 0L

    private val reasonKey get() = "VendorSubscribe_${vendorName}_Reason"
    private val codeKey get() = "VendorSubscribe_${vendorName}_Code"
    private val guideCountKey get() = "VendorSubscribe_${vendorName}_GuideCount"
    private val requestCountKey get() = "VendorSubscribe_${vendorName}_RequestCount"
    private val atKey get() = "VendorSubscribe_${vendorName}_At"
    private val settingsPendingKey get() = "VendorSubscribe_${vendorName}_SettingsPending"

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
        val skipReason = when {
            PushChannelCoordinator.getSnapshot().vendorPlatform != platform -> "not_vendor"
            PushPlatformState.getEffectivePushChannel() != PushChannel.VENDOR -> "not_vendor_channel"
            WxpAppDataService.getLoginInfo()?.deviceId.isNullOrEmpty() -> "not_login"
            !PermissionUtils.hasNotificationPermission(activity) -> "notify_off"
            !isGuideEnabled() -> "guide_off"
            else -> when (val state = currentState()) {
                VendorSubscribeState.DENIED, VendorSubscribeState.DISABLED -> null
                else -> "state_${state.name}"
            }
        }
        if (skipReason != null && skipReason != loggedSkipReason) {
            loggedSkipReason = skipReason
            log("skip_guide", "reason=$skipReason")
        }
        return skipReason == null
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
        showGuide(activity, VendorGuideSource.AUTO)
    }

    /**
     * 上报用的厂商状态：授权状态 + 授权诊断。只在发生授权操作或者开关变化时才会变，不会导致频繁上报。
     */
    fun buildVendorState(): WxpVendorState {
        logEnvOnce()
        val state = currentState()
        // 授权成功后原因清掉，之后再被关掉时看到的是 NONE + DISABLED
        if (state == VendorSubscribeState.GRANTED && WxpSaveService.get(reasonKey, "") != VendorSubscribeReason.NONE.name) {
            WxpSaveService.set(reasonKey, VendorSubscribeReason.NONE.name)
        }
        val channel = when (subscribeChannelImportance()) {
            null -> "NONE"
            NotificationManager.IMPORTANCE_NONE -> "CLOSED"
            else -> "OPEN"
        }
        val notify = runCatching {
            NotificationManagerCompat.from(ApplicationUtils.getApplication()).areNotificationsEnabled()
        }.getOrNull()
        val stateLine = "state=${state.name} channel=$channel notify=$notify"
        if (stateLine != loggedStateLine) {
            loggedStateLine = stateLine
            log("state", stateLine)
        }
        return WxpVendorState(
            subscribe = state.name,
            reason = WxpSaveService.get(reasonKey, "").ifEmpty { VendorSubscribeReason.NOT_PROMPTED.name },
            code = WxpSaveService.get(codeKey, "").ifEmpty { null },
            channel = channel,
            notify = notify,
            guide = WxpSaveService.get(guideCountKey, 0),
            request = WxpSaveService.get(requestCountKey, 0),
            at = WxpSaveService.get(atKey, 0).takeIf { it > 0 }?.toLong(),
        )
    }

    /**
     * 任意页面启动时调用：之前跳了系统设置页，回来后按实际开关记录结果。
     * 在 [PushChannelCoordinator.reportActiveIfNeeded] 之前调用，结果能跟着这次检查一起上报。
     */
    fun onActivityStarted() {
        if (!WxpSaveService.get(settingsPendingKey, false)) {
            return
        }
        WxpSaveService.set(settingsPendingKey, false)
        val state = currentState()
        val reason = if (state == VendorSubscribeState.GRANTED) VendorSubscribeReason.NONE else VendorSubscribeReason.SETTINGS_OPENED
        saveReason(reason)
        log("settings_back", "state=${state.name} reason=${reason.name}")
    }

    /** 说明弹窗显示了。 */
    protected fun recordGuideShown(source: VendorGuideSource, channelExists: Boolean) {
        val count = increase(guideCountKey)
        log("guide_show", "source=${source.name} count=$count channelExists=$channelExists state=${currentState().name}")
    }

    /**
     * 说明弹窗的按钮：confirm 去开启、dismiss 暂不、cancel 返回键关掉、switch 切换推送通道。
     * 暂不和返回键算用户拒绝了我们的说明。
     */
    protected fun recordGuideClick(action: String) {
        log("guide_click", "action=$action")
        if (action == "dismiss" || action == "cancel") {
            saveReason(VendorSubscribeReason.GUIDE_DISMISSED)
            PushChannelCoordinator.onVendorStateChanged()
        }
    }

    /** 开始拉起系统授权弹窗。 */
    protected fun recordRequestStart(detail: String = "") {
        requestStartAt = System.currentTimeMillis()
        val count = increase(requestCountKey)
        log("request_start", "count=$count $detail".trim())
    }

    /**
     * 系统授权接口返回了。reason 为空表示这次还看不出结果（比如 vivo 弹窗已经弹出、等用户操作），只记录错误码。
     */
    protected fun recordRequestResult(code: String, reason: VendorSubscribeReason?, detail: String = "") {
        val cost = if (requestStartAt > 0) System.currentTimeMillis() - requestStartAt else -1
        WxpSaveService.set(codeKey, code.take(16))
        if (reason != null) {
            saveReason(reason)
        }
        log("request_result", "code=$code reason=${reason?.name} cost=${cost}ms state=${currentState().name} $detail".trim())
    }

    /** 系统授权接口调用失败（抛异常），算其他错误。 */
    protected fun recordRequestError(throwable: Throwable) {
        WxpSaveService.set(codeKey, "exception")
        saveReason(VendorSubscribeReason.ERROR)
        WxpLogUtils.w(LOG_TAG, "event=request_error vendor=$vendorName", throwable = throwable)
    }

    /** 用户在系统弹窗里的选择（vivo 有单独的回调）。 */
    protected fun recordUserResult(reason: VendorSubscribeReason?, detail: String) {
        if (reason != null) {
            saveReason(reason)
        }
        log("user_result", "reason=${reason?.name} state=${currentState().name} $detail".trim())
    }

    /** 跳系统设置页，回来时在 [onActivityStarted] 里记录结果。 */
    protected fun recordSettingsOpened(channelId: String?, success: Boolean) {
        WxpSaveService.set(settingsPendingKey, success)
        if (success) {
            WxpSaveService.set(atKey, nowSeconds())
        }
        log("settings_open", "channelId=$channelId success=$success state=${currentState().name}")
    }

    /** 打一条授权诊断日志，格式 event=xxx vendor=xxx k=v，info 级别会上报到阿里云日志。 */
    protected fun log(event: String, detail: String) {
        WxpLogUtils.i(LOG_TAG, "event=$event vendor=$vendorName $detail")
    }

    private fun saveReason(reason: VendorSubscribeReason) {
        WxpSaveService.set(reasonKey, reason.name)
        WxpSaveService.set(atKey, nowSeconds())
    }

    private fun increase(key: String): Int {
        val count = (WxpSaveService.get(key, 0) + 1).coerceAtMost(COUNT_MAX)
        WxpSaveService.set(key, count)
        return count
    }

    private fun nowSeconds(): Int = (System.currentTimeMillis() / 1000).toInt()

    /** 读系统属性（ROM 版本），读不到返回空。 */
    protected fun systemProperty(key: String): String? = runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as? String
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** 读系统组件的版本（xmsf、vivo 推送服务），没装返回空。 */
    protected fun packageVersion(packageName: String): String? = runCatching {
        val info = ApplicationUtils.getApplication().packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        "${info.versionName}($versionCode)"
    }.getOrElse { if (it is PackageManager.NameNotFoundException) "none" else null }

    private fun logEnvOnce() {
        if (envLogged) {
            return
        }
        envLogged = true
        log(
            "env",
            "brand=${Build.MANUFACTURER} model=${Build.MODEL} sdk=${Build.VERSION.SDK_INT} " +
                "display=${Build.DISPLAY} ${vendorEnvInfo()}"
        )
    }
}
