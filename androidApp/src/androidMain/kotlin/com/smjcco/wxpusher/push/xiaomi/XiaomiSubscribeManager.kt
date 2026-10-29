package com.smjcco.wxpusher.push.xiaomi

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.config.ConfigManager
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.push.PushChannelCoordinator
import com.smjcco.wxpusher.push.VendorSubscribeGuide
import com.smjcco.wxpusher.push.VendorSubscribeState
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.ThreadUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.xiaomi.mipush.sdk.MiPushClient

/**
 * 小米资讯订阅通道「订阅的通知提醒」（158071）的授权管理。
 *
 * 小米 2026 新规：个人订阅通道 135072 在切换日关停，订阅消息必须走订阅类目通道，
 * 并且要用户在系统弹窗里授权后才会展示。通用流程见 [VendorSubscribeGuide]，
 * 方案见 monorepo docs/xiaomi-push-subscribe-template-design.md。
 */
object XiaomiSubscribeManager : VendorSubscribeGuide() {
    private const val TAG = "XiaomiSubscribe"
    private const val SUBSCRIBE_CHANNEL_ID = "158071"
    private const val LEGACY_CHANNEL_ID = "135072"

    /** 本机曾经授权成功过，用来区分「关掉了」和「从没开过」，卸载后自然清空。 */
    private const val KEY_EVER_GRANTED = "XiaomiSubscribe_EverGranted"


    // 系统弹窗返回码，见 SubscribeRemindManager
    private const val CODE_NOT_SUPPORT = -100
    private const val CODE_DEVICE_IDLE = -300
    private const val CODE_FREQUENCY_CONTROL = -500
    private const val CODE_NO_NOTIFICATION_PERMISSION = -800
    private const val CODE_NOT_SUPPORT_SYSTEM = -10001

    private var supportCache: Boolean? = null
    private var requesting = false

    override val platform = DevicePlatform.Android_XIAOMI
    override val settingTitle = "小米订阅通知授权"

    /** 升级后是否已经自动提醒过一次。 */
    override val autoPromptedKey = "XiaomiSubscribe_AutoPrompted"
    override val defaultLegacyCutoff = "2026-12-31T00:00:00+08:00"

    override fun isGuideEnabled(): Boolean = ConfigManager.getCurrentConfig().xiaomiSubscribe

    override fun legacyCutoffConfig(): String = ConfigManager.getCurrentConfig().xiaomiLegacyCutoff

    /** 本地通知类别 id，跟 MiPush SDK 创建的格式一致。 */
    private fun notificationChannelId(channelId: String): String =
        "mipush|${ApplicationUtils.getApplication().packageName}|$channelId"

    /**
     * 读取当前设备的授权状态。只在当前厂商是小米时有意义，其他厂商调用方不应该上报。
     */
    override fun currentState(): VendorSubscribeState {
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

    override fun getBannerText(): String {
        return when {
            currentState() == VendorSubscribeState.DISABLED -> "小米订阅通知已关闭，收不到推送，点击开启"
            isLegacyCutoffPassed() -> "小米订阅通知未开启，收不到推送，点击开启"
            else -> "小米推送规则调整，请开启订阅通知，否则 12 月 31 日后收不到推送"
        }
    }

    /**
     * 先弹我们自己的说明，再拉起系统授权弹窗或者跳系统设置。
     *
     * - 本地还没有订阅通道：系统弹窗里的开关默认是关的，要先提醒用户「先打开开关再点允许」；
     * - 本地有通道但被关掉了：再调授权接口不会弹窗，只能去系统设置里打开。
     */
    override fun showGuide(activity: Activity) {
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

    override fun openChannelSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannelId(SUBSCRIBE_CHANNEL_ID))
        runCatching { activity.startActivity(intent) }.onFailure {
            WxpLogUtils.w(TAG, "打开通道设置页失败", throwable = it)
            PermissionUtils.gotoNotificationSettingPage()
        }
    }
}
