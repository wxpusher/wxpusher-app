package com.smjcco.wxpusher.push

import android.app.Application
import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.biz.bean.WxpUpdateInfoReq
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigItem
import com.smjcco.wxpusher.config.ConfigManager
import com.smjcco.wxpusher.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.push.ws.connect.WsManager
import com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveServiceStarter
import com.smjcco.wxpusher.utils.ThreadUtils
import kotlinx.coroutines.launch

/**
 * 推送通道设置页使用的只读状态快照。
 *
 * 这里同时包含“用户选择”“当前实际生效结果”和“厂商通道能力”三类状态，三者含义不同：
 *
 * - [preference] 只表示用户最后一次主动选择，不会被首次注册失败或系统降级开关覆盖。
 * - [effectiveChannel] 表示最近一次已经成功生效、当前真正用于消息路由的通道。
 * - [vendorAvailability] 表示当前手机上的厂商系统推送能否被选择，与当前实际使用哪个通道无关。
 *
 * 因此在切换请求完成前，[preference] 和 [effectiveChannel] 可能不同；系统强制降级到
 * WxPusher 自建链接时，用户的 [preference] 也可能仍然是厂商系统推送。
 * 例如 `preference=VENDOR`、`effectiveChannel=WEBSOCKET`、
 * `wsReason=INITIAL_VENDOR_FALLBACK` 表示用户选择厂商推送，但因为首次注册失败，当前已降级为 WS。
 *
 * @property vendorPlatform 当前手机识别出的厂商推送平台，例如小米、华为或荣耀；
 * 不支持厂商系统推送时为 [DevicePlatform.Android]。
 * @property vendorName [vendorPlatform] 对应的用户可读名称，例如“小米系统推送”。
 * @property vendorAvailability 厂商系统推送当前的注册和可用状态，用于控制厂商选项是否可选
 * 以及展示注册中、不支持、注册失败或系统禁用等原因。
 * @property preference 用户持久化的主动选择。自动降级只改变实际通道，不修改该字段，
 * 以便厂商通道恢复可用后仍能按用户原选择自动恢复。
 * @property effectiveChannel 最近一次已经成功生效的通道。已登录设备以服务端同步成功为准；
 * 未登录设备以本地已经应用的目标为准。页面选中状态和运行时判断应使用该字段，而不是
 * 直接使用 [preference]。
 * @property effectiveName [effectiveChannel] 对应的用户可读名称；厂商通道会显示具体厂商，
 * 自建通道显示“WxPusher自建链接”。
 * @property wsReason 当前选择或请求启用 WxPusher 自建链接的原因；主动选择、首次注册失败、
 * 系统强制降级和设备不支持厂商推送会返回不同值。该字段描述“为什么目标是 WS”，不代表
 * WS 已经成功生效，是否真正生效仍以 [effectiveChannel] 为准；没有 WS 启用原因时为空。
 * @property switching 是否正在向后端同步新的平台和 token。为 `true` 时，用户选择可能已经保存，
 * 但 [effectiveChannel] 仍保持上一次成功同步的通道。
 * @property errorMessage 最近一次厂商注册、通道切换或同步失败时需要展示的错误信息；
 * 没有待展示错误时为空。
 */
data class PushChannelSnapshot(
    val vendorPlatform: DevicePlatform,
    val vendorName: String,
    val vendorAvailability: VendorAvailability,
    val preference: PushChannelPreference,
    val effectiveChannel: PushChannel,
    val effectiveName: String,
    val wsReason: WsActivationReason?,
    val switching: Boolean,
    val errorMessage: String?,
)

/**
 * Android 推送通道唯一协调器。
 *
 * 用户选择、系统配置和 token 回调只负责更新各自状态，最终统一通过
 * [syncDesiredChannel] 计算目标通道并串行同步到后端。
 */
object PushChannelCoordinator {
    private const val TAG = "PushChannelCoordinator"

    /** 等待厂商 SDK 返回 token 的最长时间。 */
    private const val VENDOR_REGISTER_TIMEOUT_MILLIS = 10_000L

    /** 自动路由同步失败后的有限退避间隔。 */
    private val AUTOMATIC_SYNC_RETRY_DELAYS_MILLIS = longArrayOf(
        30_000L,
        2 * 60_000L,
        10 * 60_000L,
    )

    private lateinit var application: Application
    private var initialized = false

    /**
     * 当前设备识别到的厂商推送平台。
     *
     * 初始化和云端配置变化时重新识别，其余协调流程复用本次结果，避免反复调用厂商 SDK
     * 能力检查，并保证一次通道决策期间平台保持一致。
     */
    private var vendorPlatform = DevicePlatform.Android

    private var vendorAvailability = VendorAvailability.REGISTERING
    private var switching = false
    private var errorMessage: String? = null

    // 同一时间只允许一个路由请求；请求期间只保留最新目标，完成后重新对账。
    private var inFlightTarget: PushChannelTarget? = null
    private var pendingTarget: PushChannelTarget? = null
    private var automaticRetryIndex = 0

    private val listeners = mutableSetOf<(PushChannelSnapshot) -> Unit>()

    private val vendorTimeoutRunnable = Runnable {
        if (vendorAvailability != VendorAvailability.REGISTERING) {
            return@Runnable
        }
        WxpLogUtils.w(TAG, "获取厂商pushToken超时，platform=${vendorPlatform.getPlatform()}")
        handleVendorTokenFailed(vendorPlatform)
    }

    private val automaticSyncRetryRunnable = Runnable {
        if (!shouldRetryAutomatically()) {
            cancelAutomaticSyncRetry()
            return@Runnable
        }
        syncDesiredChannel()
    }

    private val configListener: (ConfigItem) -> Unit = {
        onConfigChanged()
    }

    /** 初始化厂商能力、迁移旧数据，并恢复当前通道。 */
    fun init(app: Application = ApplicationUtils.getApplication()) {
        if (initialized) {
            return
        }
        initialized = true
        application = app

        vendorPlatform = PushPlatformResolver.detectVendorPushPlatform()
        PushChannelStore.migrateIfNeeded()
        restoreEffectivePlatform()
        restoreLocalEffectiveChannel()
        ConfigManager.addListener(configListener)
        applyCurrentDecision()
    }

    /** 从持久化状态恢复运行时生效平台和旧业务仍使用的通用 token。 */
    private fun restoreEffectivePlatform() {
        val defaultPlatform = if (getDesiredChannel() == PushChannel.WEBSOCKET) {
            DevicePlatform.Android
        } else {
            vendorPlatform
        }
        val savedTarget = PushPlatformState.restoreEffectivePushPlatform(defaultPlatform)
        if (savedTarget != null) {
            WxpAppDataService.savePushToken(savedTarget.token)
        }
    }

    /** 按服务端最后确认的通道恢复本地 WS 服务，避免进程重启后出现接收空窗。 */
    private fun restoreLocalEffectiveChannel() {
        val effectiveTarget = PushPlatformState.getPersistedEffectivePushTarget()
        if (
            effectiveTarget?.platform == DevicePlatform.Android
            && effectiveTarget.token.isNotEmpty()
        ) {
            ensureWsRunning()
        } else if (PushChannelStore.isWsRequested()) {
            stopWsInfrastructure()
        }
    }

    /** 根据设备能力、ConfigManager 和用户选择执行一次完整启动决策。 */
    private fun applyCurrentDecision() {
        cancelAutomaticSyncRetry()

        if (!hasVendorSupport()) {
            vendorAvailability = VendorAvailability.UNSUPPORTED
            PushChannelStore.setWsReason(WsActivationReason.UNSUPPORTED_DEVICE)
            ensureWsRunning()
            syncDesiredChannel(force = true)
            return
        }

        if (!isVendorAllowed()) {
            vendorAvailability = VendorAvailability.CONFIG_DISABLED
            PushChannelStore.setWsReason(WsActivationReason.SYSTEM_CONFIG_FORCED)
            ensureWsRunning()
            syncDesiredChannel(force = true)
            return
        }

        clearExpiredForcedWsReason()
        val vendorTarget = getCurrentVendorTarget()
        vendorAvailability = if (vendorTarget == null) {
            VendorAvailability.REGISTERING
        } else {
            VendorAvailability.READY
        }

        if (PushChannelStore.getPreference() == PushChannelPreference.WEBSOCKET) {
            PushChannelStore.setWsReason(WsActivationReason.USER_SELECTED)
            ensureWsRunning()
            startVendorRegistration()
            syncDesiredChannel(force = true)
            return
        }

        if (PushChannelStore.getWsReason() == WsActivationReason.USER_SELECTED) {
            PushChannelStore.setWsReason(null)
        }

        val effectiveTarget = PushPlatformState.getPersistedEffectivePushTarget()
        if (
            vendorTarget == null
            && effectiveTarget?.platform == DevicePlatform.Android
            && effectiveTarget.token.isNotEmpty()
        ) {
            // 已经生效的 WS 在新厂商 token 返回前继续工作，成功后自动恢复厂商通道。
            PushChannelStore.setWsReason(WsActivationReason.INITIAL_VENDOR_FALLBACK)
            ensureWsRunning()
        }

        startVendorRegistration()
        syncDesiredChannel(force = true)
    }

    /** 厂商 SDK 成功返回 token 后的统一入口。 */
    fun onVendorToken(token: String, platform: DevicePlatform) {
        ThreadUtils.runOnMainThread {
            handleVendorToken(token, platform)
        }
    }

    /** 在主线程校验并处理厂商 token。 */
    private fun handleVendorToken(token: String, platform: DevicePlatform) {
        if (token.isEmpty() || platform != vendorPlatform) {
            WxpLogUtils.i(
                TAG,
                "忽略非当前厂商token，callback=${platform.getPlatform()}, vendor=${vendorPlatform.getPlatform()}"
            )
            return
        }

        ThreadUtils.getMainThreadHandler().removeCallbacks(vendorTimeoutRunnable)
        PushChannelStore.saveVendorRegistration(platform, token)
        // 产品定义以成功取得合法厂商 token 为“曾注册成功”，不依赖后端同步结果。
        PushChannelStore.setVendorEverRegistered(true)
        vendorAvailability = if (isVendorAllowed()) {
            VendorAvailability.READY
        } else {
            VendorAvailability.CONFIG_DISABLED
        }
        errorMessage = null
        cancelAutomaticSyncRetry()

        if (getDesiredChannel() == PushChannel.VENDOR) {
            syncDesiredChannel()
        } else {
            // 当前目标是 WS 时仅缓存厂商 token，方便以后手动或自动恢复。
            notifyChanged()
        }
    }

    /** 厂商 SDK 注册失败或等待 token 超时后的统一入口。 */
    fun onVendorTokenFailed(platform: DevicePlatform) {
        ThreadUtils.runOnMainThread {
            handleVendorTokenFailed(platform)
        }
    }

    /** 在主线程校验并处理厂商注册失败。 */
    private fun handleVendorTokenFailed(platform: DevicePlatform) {
        if (platform != vendorPlatform) {
            WxpLogUtils.i(
                TAG,
                "忽略非当前厂商注册失败，callback=${platform.getPlatform()}, vendor=${vendorPlatform.getPlatform()}"
            )
            return
        }

        ThreadUtils.getMainThreadHandler().removeCallbacks(vendorTimeoutRunnable)
        if (!isVendorAllowed()) {
            vendorAvailability = VendorAvailability.CONFIG_DISABLED
            notifyChanged()
            return
        }

        vendorAvailability = VendorAvailability.REGISTER_FAILED
        errorMessage = "注册系统推送失败"

        // 只有从未成功取得过厂商 token 时才自动降级，后续异常交由用户手动选择。
        if (
            !PushChannelStore.hasVendorEverRegistered()
            && PushChannelStore.getPreference() == PushChannelPreference.VENDOR
        ) {
            PushChannelStore.setWsReason(WsActivationReason.INITIAL_VENDOR_FALLBACK)
            ensureWsRunning()
            syncDesiredChannel()
        }
        notifyChanged()
    }

    /** WebSocket 初始化消息返回 token 后，仅在当前目标为 WS 时同步路由。 */
    fun onWsToken(token: String) {
        ThreadUtils.runOnMainThread {
            if (token.isEmpty()) {
                return@runOnMainThread
            }
            PushChannelStore.setWsToken(token)
            if (getDesiredChannel() == PushChannel.WEBSOCKET) {
                syncDesiredChannel()
            }
        }
    }

    /** 用户在设置页选择厂商系统推送。 */
    fun selectVendor() {
        if (switching || vendorAvailability != VendorAvailability.READY || !isVendorAllowed()) {
            return
        }

        val vendorTarget = getCurrentVendorTarget()
        if (vendorTarget == null) {
            vendorAvailability = VendorAvailability.REGISTERING
            startVendorRegistration()
            notifyChanged()
            return
        }

        // 用户选择立即持久化；页面选中状态仍以服务端已确认的 effectiveTarget 为准。
        PushChannelStore.setPreference(PushChannelPreference.VENDOR)
        PushChannelStore.setWsReason(null)
        errorMessage = null
        cancelAutomaticSyncRetry()
        syncDesiredChannel()
    }

    /** 用户在设置页选择 WxPusher 自建链接。 */
    fun selectWebSocket() {
        if (switching) {
            return
        }

        PushChannelStore.setPreference(PushChannelPreference.WEBSOCKET)
        PushChannelStore.setWsReason(WsActivationReason.USER_SELECTED)
        errorMessage = null
        cancelAutomaticSyncRetry()
        ensureWsRunning()
        syncDesiredChannel()
    }

    /**
     * Activity 启动时检查设备活跃上报。
     *
     * 该方法会在每次页面切换时被调用，因此依赖最近一次成功上报时间做一小时节流。
     * 超过间隔后仍通过协调器成对上报当前平台和 token，避免独立上报再次造成
     * platform 与 token 错配。只有已经登录且存在可用目标时才会真正发起请求。
     */
    fun reportActiveIfNeeded() {
        ThreadUtils.runOnMainThread {
            if (!initialized || WxpAppDataService.getLoginInfo()?.deviceId.isNullOrEmpty()) {
                return@runOnMainThread
            }

            if (!WxpAppDataService.isDeviceInfoReportExpired()) {
                return@runOnMainThread
            }

            WxpLogUtils.i(TAG, "Android回到页面，重新上报当前推送路由和设备活跃信息")
            syncDesiredChannel(force = true)
        }
    }

    /** 用户点击“重新注册”时重试厂商 SDK，但不改变当前推送通道。 */
    fun retryVendorRegistration() {
        if (!hasVendorSupport() || !isVendorAllowed() || switching) {
            return
        }
        vendorAvailability = VendorAvailability.REGISTERING
        errorMessage = null
        startVendorRegistration()
        notifyChanged()
    }

    /** 发起厂商注册；仅在页面处于注册中时安装超时，避免 SDK 永不回调。 */
    private fun startVendorRegistration() {
        if (!hasVendorSupport() || !isVendorAllowed()) {
            return
        }

        if (
            getCurrentVendorTarget() == null
            || vendorAvailability == VendorAvailability.REGISTER_FAILED
        ) {
            vendorAvailability = VendorAvailability.REGISTERING
        }

        PushManager.startVendorRegistration(application, vendorPlatform)
        ThreadUtils.getMainThreadHandler().removeCallbacks(vendorTimeoutRunnable)
        if (vendorAvailability == VendorAvailability.REGISTERING) {
            ThreadUtils.runOnMainThread(
                vendorTimeoutRunnable,
                VENDOR_REGISTER_TIMEOUT_MILLIS,
            )
        }
        notifyChanged()
    }

    /** 确保 WS 连接与保活服务已经启动。 */
    private fun ensureWsRunning() {
        PushChannelStore.setWsRequested(true)
        WxpNotificationManager.init()
        WsManager.start()
        KeepWsAliveServiceStarter.start(application)
    }

    /**
     * 计算最新目标并与后端对账。
     *
     * 已有请求时不并发写入，只记录最新目标；当前请求结束后会重新计算并继续同步，
     * 因此不会丢失用户最后一次选择。
     */
    private fun syncDesiredChannel(force: Boolean = false) {
        val desiredTarget = buildDesiredTarget()
        if (desiredTarget == null) {
            notifyChanged()
            return
        }

        if (inFlightTarget != null) {
            pendingTarget = desiredTarget
            return
        }

        val effectiveTarget = PushPlatformState.getPersistedEffectivePushTarget()
        if (!force && effectiveTarget == desiredTarget) {
            applyLocalChannelState(desiredTarget)
            return
        }

        submitTarget(desiredTarget)
    }

    /** 串行提交一个明确的平台和 token。 */
    private fun submitTarget(target: PushChannelTarget) {
        val deviceUuid = WxpAppDataService.getLoginInfo()?.deviceId
        if (deviceUuid.isNullOrEmpty()) {
            applyTargetBeforeLogin(target)
            return
        }

        inFlightTarget = target
        switching = true
        errorMessage = null
        notifyChanged()

        val updateInfoReq = WxpUpdateInfoReq(
            deviceUuid = deviceUuid,
            pushToken = target.token,
            platform = target.platform.getPlatform(),
        )
        WxpScopeUtils.getMainScope().launch {
            val success = WxpApiService.updateDeviceInfo(req = updateInfoReq, silent = true) == true
            if (success) {
                // 只有服务端明确返回成功才记录上报状态，网络失败后下次回到页面仍可继续尝试。
                WxpAppDataService.recordDeviceInfoReportSuccess(updateInfoReq)
            }
            onTargetSubmitted(target, success)
        }
    }

    /**
     * 未登录时在本地应用目标通道，为后续登录请求准备成对的平台和 token。
     *
     * 此时服务端尚未收到设备信息，因此不能调用 [onTargetSubmitted] 伪装请求成功，
     * 也绝不能更新最近成功上报时间。登录请求会携带这里保存的平台和 token 完成设备注册。
     */
    private fun applyTargetBeforeLogin(target: PushChannelTarget) {
        PushPlatformState.commitEffectivePushTarget(
            target.platform,
            target.token,
        )
        WxpAppDataService.savePushToken(target.token)
        applyLocalChannelState(target)
    }

    /** 处理一次路由上报结果，并在请求期间目标变化时继续同步最新目标。 */
    private fun onTargetSubmitted(submittedTarget: PushChannelTarget, success: Boolean) {
        if (inFlightTarget != submittedTarget) {
            return
        }

        inFlightTarget = null
        val queuedTarget = pendingTarget
        pendingTarget = null

        if (success) {
            // 每次成功都先记录服务端真实状态；即使它已经过期，也能保证进程被杀后本地与后端一致。
            PushPlatformState.commitEffectivePushTarget(
                submittedTarget.platform,
                submittedTarget.token,
            )
            WxpAppDataService.savePushToken(submittedTarget.token)

            val latestTarget = buildDesiredTarget()
            if (latestTarget == submittedTarget) {
                applyLocalChannelState(submittedTarget)
                return
            }

            WxpLogUtils.i(
                TAG,
                "路由请求完成后目标已变化，submitted=$submittedTarget, queued=$queuedTarget, latest=$latestTarget"
            )
            if (latestTarget != null) {
                submitTarget(latestTarget)
            } else {
                switching = false
                notifyChanged()
            }
            return
        }

        val latestTarget = buildDesiredTarget()
        if (latestTarget != null && latestTarget != submittedTarget) {
            // 旧目标失败不影响新目标，立即继续提交当前最新选择。
            submitTarget(latestTarget)
            return
        }

        switching = false
        errorMessage = "切换失败，请稍后重试"
        if (
            submittedTarget.platform == DevicePlatform.Android
            && PushChannelStore.getWsReason() == WsActivationReason.USER_SELECTED
        ) {
            // 用户主动切换失败后停止无效 WS，保留 preference 供下次手动重试。
            stopWsInfrastructure()
        }

        if (shouldRetryAutomatically()) {
            scheduleAutomaticSyncRetry()
        }
        notifyChanged()
    }

    /** 服务端确认目标仍是最新选择后，更新本地通道和设置页。 */
    private fun applyLocalChannelState(target: PushChannelTarget) {
        if (buildDesiredTarget() != target) {
            syncDesiredChannel()
            return
        }

        if (target.platform == DevicePlatform.Android) {
            PushChannelStore.setWsToken(target.token)
            ensureWsRunning()
        } else {
            PushChannelStore.saveVendorRegistration(target.platform, target.token)
            PushChannelStore.setVendorEverRegistered(true)
            PushChannelStore.setWsReason(null)
            stopWsInfrastructure()
        }

        switching = false
        errorMessage = null
        cancelAutomaticSyncRetry()
        PushManager.notifyEffectiveTokenChanged(target.platform, target.token)
        notifyChanged()
    }

    /** 停止 WS 连接、保活服务和尚未执行的启动任务。 */
    private fun stopWsInfrastructure() {
        PushChannelStore.setWsRequested(false)
        WsManager.stop()
        KeepWsAliveServiceStarter(application).stop()
    }

    /** 按有限退避间隔安排下一次系统自动同步。 */
    private fun scheduleAutomaticSyncRetry() {
        ThreadUtils.getMainThreadHandler().removeCallbacks(automaticSyncRetryRunnable)
        if (automaticRetryIndex >= AUTOMATIC_SYNC_RETRY_DELAYS_MILLIS.size) {
            WxpLogUtils.w(TAG, "自动同步推送通道已达到最大重试次数")
            return
        }

        val delayMillis = AUTOMATIC_SYNC_RETRY_DELAYS_MILLIS[automaticRetryIndex]
        automaticRetryIndex += 1
        ThreadUtils.runOnMainThread(automaticSyncRetryRunnable, delayMillis)
    }

    /** 取消自动同步任务并重置退避次数。 */
    private fun cancelAutomaticSyncRetry() {
        ThreadUtils.getMainThreadHandler().removeCallbacks(automaticSyncRetryRunnable)
        automaticRetryIndex = 0
    }

    /** 只有系统兜底行为失败时才自动重试，用户手动切换失败交由用户再次操作。 */
    private fun shouldRetryAutomatically(): Boolean {
        return when (PushChannelStore.getWsReason()) {
            WsActivationReason.INITIAL_VENDOR_FALLBACK,
            WsActivationReason.SYSTEM_CONFIG_FORCED,
            WsActivationReason.UNSUPPORTED_DEVICE -> true

            WsActivationReason.USER_SELECTED,
            null -> false
        }
    }

    /** 云端配置变化后重新识别能力，并按用户原选择重新对账。 */
    private fun onConfigChanged() {
        cancelAutomaticSyncRetry()
        vendorPlatform = PushPlatformResolver.detectVendorPushPlatform()

        if (!hasVendorSupport()) {
            vendorAvailability = VendorAvailability.UNSUPPORTED
            PushChannelStore.setWsReason(WsActivationReason.UNSUPPORTED_DEVICE)
            ensureWsRunning()
            syncDesiredChannel()
            return
        }

        if (!isVendorAllowed()) {
            ThreadUtils.getMainThreadHandler().removeCallbacks(vendorTimeoutRunnable)
            vendorAvailability = VendorAvailability.CONFIG_DISABLED
            errorMessage = null
            PushChannelStore.setWsReason(WsActivationReason.SYSTEM_CONFIG_FORCED)
            ensureWsRunning()
            syncDesiredChannel()
            return
        }

        clearExpiredForcedWsReason()
        val vendorTarget = getCurrentVendorTarget()
        vendorAvailability = if (vendorTarget == null) {
            VendorAvailability.REGISTERING
        } else {
            VendorAvailability.READY
        }

        if (PushChannelStore.getPreference() == PushChannelPreference.WEBSOCKET) {
            PushChannelStore.setWsReason(WsActivationReason.USER_SELECTED)
            ensureWsRunning()
        } else {
            if (PushChannelStore.getWsReason() == WsActivationReason.USER_SELECTED) {
                PushChannelStore.setWsReason(null)
            }
            if (
                vendorTarget == null
                && PushPlatformState.getPersistedEffectivePushTarget()?.platform
                == DevicePlatform.Android
            ) {
                PushChannelStore.setWsReason(WsActivationReason.INITIAL_VENDOR_FALLBACK)
                ensureWsRunning()
            }
        }

        startVendorRegistration()
        syncDesiredChannel()
    }

    /** 清除已经不再成立的系统强制原因，保留用户主动选择。 */
    private fun clearExpiredForcedWsReason() {
        val reason = PushChannelStore.getWsReason()
        if (
            reason == WsActivationReason.SYSTEM_CONFIG_FORCED
            || reason == WsActivationReason.UNSUPPORTED_DEVICE
        ) {
            val restoredReason = if (
                PushChannelStore.getPreference() == PushChannelPreference.WEBSOCKET
            ) {
                WsActivationReason.USER_SELECTED
            } else {
                null
            }
            PushChannelStore.setWsReason(restoredReason)
        }
    }

    /** 根据设备能力、系统配置和用户选择计算当前目标通道。 */
    private fun getDesiredChannel(): PushChannel {
        if (!hasVendorSupport() || !isVendorAllowed()) {
            return PushChannel.WEBSOCKET
        }
        if (PushChannelStore.getPreference() == PushChannelPreference.WEBSOCKET) {
            return PushChannel.WEBSOCKET
        }
        if (
            PushChannelStore.getWsReason() == WsActivationReason.INITIAL_VENDOR_FALLBACK
            && getCurrentVendorTarget() == null
        ) {
            return PushChannel.WEBSOCKET
        }
        return PushChannel.VENDOR
    }

    /** 将当前目标通道转换成必须成对上报的平台和 token。 */
    private fun buildDesiredTarget(): PushChannelTarget? {
        return if (getDesiredChannel() == PushChannel.WEBSOCKET) {
            val token = PushChannelStore.getWsToken()
            if (token.isEmpty()) {
                null
            } else {
                PushChannelTarget(DevicePlatform.Android, token)
            }
        } else {
            getCurrentVendorTarget()
        }
    }

    /** 读取与当前设备厂商匹配的缓存 token，发现错配时立即清理。 */
    private fun getCurrentVendorTarget(): PushChannelTarget? {
        val target = PushChannelStore.getVendorPushTarget() ?: return null
        if (target.platform == vendorPlatform) {
            return target
        }

        WxpLogUtils.w(
            TAG,
            "清理平台不匹配的厂商token，cached=${target.platform}, vendor=$vendorPlatform"
        )
        PushChannelStore.clearVendorRegistration()
        PushChannelStore.setVendorEverRegistered(false)
        return null
    }

    /** 注册设置页状态监听器。 */
    fun addListener(listener: (PushChannelSnapshot) -> Unit) {
        listeners.add(listener)
        listener(getSnapshot())
    }

    /** 移除设置页状态监听器。 */
    fun removeListener(listener: (PushChannelSnapshot) -> Unit) {
        listeners.remove(listener)
    }

    /** 获取设置页展示所需的当前只读状态。 */
    fun getSnapshot(): PushChannelSnapshot {
        val effectivePlatform = PushPlatformState.getEffectivePushPlatform()
        val effectiveChannel = PushPlatformState.getEffectivePushChannel()
        return PushChannelSnapshot(
            vendorPlatform = vendorPlatform,
            vendorName = getVendorName(vendorPlatform),
            vendorAvailability = vendorAvailability,
            preference = PushChannelStore.getPreference(),
            effectiveChannel = effectiveChannel,
            effectiveName = if (effectiveChannel == PushChannel.WEBSOCKET) {
                "WxPusher自建链接"
            } else {
                getVendorName(effectivePlatform)
            },
            wsReason = PushChannelStore.getWsReason(),
            switching = switching,
            errorMessage = errorMessage,
        )
    }

    /** 获取当前已经由后端确认生效的通道名称。 */
    fun getCurrentChannelName(): String = getSnapshot().effectiveName

    /** 将最新快照统一回调到主线程。 */
    private fun notifyChanged() {
        val snapshot = getSnapshot()
        ThreadUtils.runOnMainThread {
            listeners.toList().forEach { it(snapshot) }
        }
    }

    /** 判断当前手机是否识别到厂商系统推送能力。 */
    private fun hasVendorSupport(): Boolean = vendorPlatform != DevicePlatform.Android

    /** 判断当前识别到的厂商推送是否被 ConfigManager 允许使用。 */
    private fun isVendorAllowed(): Boolean {
        return PushPlatformResolver.isVendorPushAllowed(vendorPlatform)
    }

    /** 返回设置页展示的厂商推送名称。 */
    fun getVendorName(platform: DevicePlatform): String = when (platform) {
        DevicePlatform.Android_XIAOMI -> "小米系统推送"
        DevicePlatform.Android_HUAWEI -> "华为系统推送"
        DevicePlatform.Android_VIVO -> "VIVO 系统推送"
        DevicePlatform.Android_HONOR -> "荣耀系统推送"
        DevicePlatform.Android_OPPO -> "OPPO 系统推送"
        DevicePlatform.Android_MEIZU -> "魅族系统推送"
        else -> "系统推送"
    }
}
