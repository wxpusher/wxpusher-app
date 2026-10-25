package com.smjcco.wxpusher.push.xiaomi

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigManager
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.push.PushChannel
import com.smjcco.wxpusher.push.PushChannelCoordinator
import com.smjcco.wxpusher.push.PushPlatformState
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.ThreadUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.xiaomi.mipush.sdk.MiPushClient
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
 * 小米资讯订阅通道「订阅的通知提醒」（158071）的授权管理。
 *
 * 小米 2026 新规：个人订阅通道 135072 在切换日关停，订阅消息必须走订阅类目通道，
 * 并且要用户在系统弹窗里授权后才会展示。授权状态一律以本地通知类别的实际开关为准，
 * 通过 [PushChannelCoordinator] 跟 platform / token 一起上报，服务端按状态选通道。
 * 方案见 monorepo docs/xiaomi-push-subscribe-template-design.md。
 */
object XiaomiSubscribeManager {
    private const val TAG = "XiaomiSubscribe"
    private const val SUBSCRIBE_CHANNEL_ID = "158071"
    private const val LEGACY_CHANNEL_ID = "135072"
    private const val DEFAULT_LEGACY_CUTOFF = "2026-12-31T00:00:00+08:00"

    /** 切换日前多少天起，每次冷启动都提醒一次。 */
    private const val REMIND_EVERY_LAUNCH_BEFORE_CUTOFF_MILLIS = 7 * 24 * 3600_000L

    /** 本机曾经授权成功过，用来区分「关掉了」和「从没开过」，卸载后自然清空。 */
    private const val KEY_EVER_GRANTED = "XiaomiSubscribe_EverGranted"

    /** 升级后是否已经自动提醒过一次。 */
    private const val KEY_AUTO_PROMPTED = "XiaomiSubscribe_AutoPrompted"

    // 系统弹窗返回码，见 SubscribeRemindManager
    private const val CODE_NOT_SUPPORT = -100
    private const val CODE_DEVICE_IDLE = -300
    private const val CODE_FREQUENCY_CONTROL = -500
    private const val CODE_NO_NOTIFICATION_PERMISSION = -800
    private const val CODE_NOT_SUPPORT_SYSTEM = -10001

    private var supportCache: Boolean? = null
    private var autoPromptedThisProcess = false
    private var requesting = false

    /** 本地通知类别 id，跟 MiPush SDK 创建的格式一致。 */
    private fun notificationChannelId(channelId: String): String =
        "mipush|${ApplicationUtils.getApplication().packageName}|$channelId"

    /**
     * 读取当前设备的授权状态。只在当前厂商是小米时有意义，其他厂商调用方不应该上报。
     */
    fun currentState(): VendorSubscribeState {
        if (!isSupported()) {
            return VendorSubscribeState.UNSUPPORTED
        }
        val importance = getSubscribeChannelImportance()
        if (importance != null && importance != NotificationManager.IMPORTANCE_NONE) {
            WxpSaveService.set(KEY_EVER_GRANTED, true)
            return VendorSubscribeState.GRANTED
        }
        // 拒绝弹窗时系统可能也会建一个关着的通道，只有本机授权过才算「关掉了」
        return if (importance != null && WxpSaveService.get(KEY_EVER_GRANTED, false)) {
            VendorSubscribeState.DISABLED
        } else {
            VendorSubscribeState.DENIED
        }
    }

    /** 本地通知类别的 importance，通道不存在时返回空。 */
    private fun getSubscribeChannelImportance(): Int? {
        val manager = ApplicationUtils.getApplication()
            .getSystemService(NotificationManager::class.java) ?: return null
        return manager.getNotificationChannel(notificationChannelId(SUBSCRIBE_CHANNEL_ID))?.importance
    }

    /** 系统是否支持订阅功能；读的是 xmsf 的 meta-data，进程内缓存。 */
    private fun isSupported(): Boolean {
        supportCache?.let { return it }
        val supported = runCatching {
            MiPushClient.checkSubscribeChannelSupport(ApplicationUtils.getApplication())
        }.getOrDefault(false)
        supportCache = supported
        return supported
    }

    /** 老通道 135072 是否已经关停，切换日从远程配置读。 */
    fun isLegacyCutoffPassed(): Boolean = System.currentTimeMillis() >= getLegacyCutoffTime()

    private fun getLegacyCutoffTime(): Long {
        val cutoff = ConfigManager.getCurrentConfig().xiaomiLegacyCutoff
        return runCatching { OffsetDateTime.parse(cutoff).toInstant().toEpochMilli() }
            .getOrElse { OffsetDateTime.parse(DEFAULT_LEGACY_CUTOFF).toInstant().toEpochMilli() }
    }

    /**
     * 当前实际在用的小米通知类别 id：授权过或者已经过了切换日，就是新通道，否则是老通道。
     * 铃声引导、通知设置引导都要指向这个通道。
     */
    fun getActiveNotificationChannelId(): String {
        val state = currentState()
        val useSubscribe = state == VendorSubscribeState.GRANTED
            || state == VendorSubscribeState.DISABLED
            || isLegacyCutoffPassed()
        return notificationChannelId(if (useSubscribe) SUBSCRIBE_CHANNEL_ID else LEGACY_CHANNEL_ID)
    }

    /** 当前通道对应的系统设置里的名字。 */
    fun getActiveNotificationChannelName(): String {
        return if (getActiveNotificationChannelId().endsWith(SUBSCRIBE_CHANNEL_ID)) {
            "订阅的通知提醒"
        } else {
            "订阅消息"
        }
    }

    /**
     * 是否需要引导用户开启：小米设备、当前在用厂商推送、已登录、应用通知已开、
     * 远程配置打开，并且状态是没授权或者被关掉了。
     */
    fun needGuide(activity: Activity?): Boolean {
        if (PushChannelCoordinator.getSnapshot().vendorPlatform != DevicePlatform.Android_XIAOMI) {
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
        if (!ConfigManager.getCurrentConfig().xiaomiSubscribe) {
            return false
        }
        val state = currentState()
        return state == VendorSubscribeState.DENIED || state == VendorSubscribeState.DISABLED
    }

    /** 消息列表顶部 banner 的提示文案。 */
    fun getBannerText(): String {
        return when {
            currentState() == VendorSubscribeState.DISABLED -> "小米订阅通知已关闭，收不到推送，点击开启"
            isLegacyCutoffPassed() -> "小米订阅通知未开启，收不到推送，点击开启"
            else -> "小米推送规则调整，请开启订阅通知，否则 12 月 31 日后收不到推送"
        }
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
        if (WxpSaveService.get(KEY_AUTO_PROMPTED, false) && !nearCutoff) {
            return
        }
        autoPromptedThisProcess = true
        WxpSaveService.set(KEY_AUTO_PROMPTED, true)
        showGuide(activity)
    }

    /**
     * 先弹我们自己的说明，再拉起系统授权弹窗或者跳系统设置。
     *
     * - 本地还没有订阅通道：系统弹窗里的开关默认是关的，要先提醒用户「先打开开关再点允许」；
     * - 本地有通道但被关掉了：再调授权接口不会弹窗，只能去系统设置里打开。
     */
    fun showGuide(activity: Activity) {
        if (requesting || activity.isFinishing || activity.isDestroyed) {
            return
        }
        val channelExists = getSubscribeChannelImportance() != null
        val message = if (channelExists) {
            "「订阅的通知提醒」已被关闭，关闭后收不到推送。\n\n请在接下来的设置页里打开「允许通知」开关。"
        } else {
            "小米推送规则调整，需要开启「订阅的通知提醒」才能正常收到推送。\n\n接下来的系统弹窗里，请先打开「订阅的通知提醒」开关，再点「允许」。"
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle("开启小米订阅通知")
            .setMessage(message)
            .setPositiveButton("去开启") { dialog, _ ->
                dialog.dismiss()
                if (channelExists) {
                    openChannelSettings(activity)
                } else {
                    requestAuthorization(activity)
                }
            }
            .setNegativeButton("暂不") { dialog, _ -> dialog.dismiss() }
        if (isLegacyCutoffPassed()) {
            builder.setNeutralButton("切换推送通道") { dialog, _ ->
                dialog.dismiss()
                WxpJumpPageUtils.jumpToPushChannelSetting(activity)
            }
        }
        DialogManager.show(activity, builder.create())
    }

    /** 调系统授权弹窗。频控（30 秒 1 次、单通道每月 2 次）由系统执行，超限回调 -500。 */
    private fun requestAuthorization(activity: Activity) {
        requesting = true
        runCatching {
            MiPushClient.requestSubscribeChannel(activity, arrayListOf(SUBSCRIBE_CHANNEL_ID)) { code, bundle ->
                ThreadUtils.runOnMainThread {
                    requesting = false
                    WxpLogUtils.i(TAG, "授权回调，code=$code, result=${bundle?.get("open_channel_result")}")
                    onAuthorizationResult(activity, code)
                }
            }
        }.onFailure {
            requesting = false
            WxpLogUtils.w(TAG, "调用授权接口失败", throwable = it)
        }
    }

    private fun onAuthorizationResult(activity: Activity, code: Int) {
        if (code == CODE_NOT_SUPPORT || code == CODE_NOT_SUPPORT_SYSTEM) {
            supportCache = false
        }
        // 以本地通道的实际开关为准，回调只是重新检测的时机
        PushChannelCoordinator.onVendorStateChanged()
        val state = currentState()
        if (state == VendorSubscribeState.GRANTED) {
            WxpToastUtils.showToast("已开启。之前设置的提醒铃声需要在「推送通道设置」里重新设置")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        when (code) {
            // 不在前台，等下次回到前台再提醒
            CODE_DEVICE_IDLE -> return
            CODE_NO_NOTIFICATION_PERMISSION -> {
                WxpToastUtils.showToast("请先打开应用的通知权限")
                PermissionUtils.gotoNotificationSettingPage()
                return
            }
        }
        if (getSubscribeChannelImportance() != null) {
            openChannelSettings(activity)
            return
        }
        // 没有通道，系统设置里没有可以打开的开关
        val message = if (code == CODE_FREQUENCY_CONTROL) {
            "本月授权次数已用完，可以下个月再试，也可以切换到 WebSocket 推送。"
        } else {
            "没有开启小米订阅通知，可能收不到推送。可以稍后再试，也可以切换到 WebSocket 推送。"
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("未开启小米订阅通知")
            .setMessage(message)
            .setPositiveButton("切换推送通道") { d, _ ->
                d.dismiss()
                WxpJumpPageUtils.jumpToPushChannelSetting(activity)
            }
            .setNegativeButton("知道了") { d, _ -> d.dismiss() }
            .create()
        DialogManager.show(activity, dialog)
    }

    /** 打开订阅通道的系统设置页。 */
    fun openChannelSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannelId(SUBSCRIBE_CHANNEL_ID))
        runCatching { activity.startActivity(intent) }.onFailure {
            WxpLogUtils.w(TAG, "打开通道设置页失败", throwable = it)
            PermissionUtils.gotoNotificationSettingPage()
        }
    }
}
