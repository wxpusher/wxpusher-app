package com.smjcco.wxpusher.push.vivo

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
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
import com.vivo.push.PushClient
import com.vivo.push.subscribedialog.ChannelOperation

/**
 * vivo 订阅渠道「订阅内容更新」（vpush_channel_60）的授权管理。
 *
 * vivo 新规：2027-04-01 起，订阅提醒类消息必须用模板发送，用户在 vivo 系统弹窗里授权订阅渠道后才会展示。
 * 通用流程见 [VendorSubscribeGuide]，方案见 monorepo docs/vivo-push-subscribe-template-design.md 5.4 节。
 *
 * 状态只按本地通知渠道的开关判断：弹窗接口返回 6048 只说明渠道「已创建」（拒绝 3 次后 vivo 也会建一个关着的），
 * 用户操作回调在进程重启后就没了，所以这两个都只当作重新读取状态的时机。
 */
object VivoSubscribeManager : VendorSubscribeGuide() {
    private const val TAG = "VivoSubscribe"

    /** vivo 后台创建的订阅渠道，关联模板 t1-34veh6-1i4。 */
    private const val SUBSCRIBE_CHANNEL_ID = "vpush_channel_60"
    private const val SUBSCRIBE_CHANNEL_NAME = "订阅内容更新"

    /** vpush 引擎是否支持订阅弹窗的能力位。 */
    private const val SUPPORT_SUBSCRIBE_DIALOG_FLAG = 1 shl 21

    /** 本机曾经授权成功过，用来区分「关掉了」和「从没开过」，卸载后自然清空。 */
    private const val KEY_EVER_GRANTED = "VivoSubscribe_EverGranted"

    // showSubscribeDialog 的返回码，见《vpush消息模板接入指南》5.7
    private const val CODE_SHOWN = 0
    private const val CODE_ANDROID_VERSION_NOT_SUPPORT = 8108
    private const val CODE_ENGINE_NOT_SUPPORT = 8109
    private const val CODE_NOTIFICATION_OFF = 8110
    private const val CODE_API_FREQUENCY = 8111
    private const val CODE_DIALOG_FREQUENCY = 6043
    private const val CODE_NOT_FOREGROUND = 6044
    private const val CODE_NOTIFICATION_OFF_ENGINE = 6045
    private const val CODE_ALL_CHANNEL_CREATED = 6048
    private const val CODE_DIALOG_SHOWING = 6050

    private var supportCache: Boolean? = null
    private var requesting = false

    /** 找到的本地渠道 id，只打一次日志，方便真机上确认 vivo 创建渠道的格式。 */
    private var loggedChannelId: String? = null

    override val platform = DevicePlatform.Android_VIVO
    override val settingTitle = "vivo订阅通知授权"

    /** 升级后是否已经自动提醒过一次。 */
    override val autoPromptedKey = "VivoSubscribe_AutoPrompted"
    override val defaultLegacyCutoff = "2027-04-01T00:00:00+08:00"

    override fun isGuideEnabled(): Boolean = ConfigManager.getCurrentConfig().vivoSubscribe

    override fun legacyCutoffConfig(): String = ConfigManager.getCurrentConfig().vivoLegacyCutoff

    /**
     * 读取当前设备的授权状态。只在当前厂商是 vivo 时有意义，其他厂商调用方不应该上报。
     * 拿不准时报 DENIED，不报 DISABLED：报成 DISABLED 服务端会直接不发，消息就丢了。
     */
    override fun currentState(): VendorSubscribeState {
        if (!isSupported()) {
            return VendorSubscribeState.UNSUPPORTED
        }
        val importance = findSubscribeChannel()?.importance
        if (importance != null && importance != NotificationManager.IMPORTANCE_NONE) {
            WxpSaveService.set(KEY_EVER_GRANTED, true)
            return VendorSubscribeState.GRANTED
        }
        // 拒绝超过 3 次 vivo 会建一个关着的渠道，只有本机授权过才算「关掉了」
        return if (importance != null && WxpSaveService.get(KEY_EVER_GRANTED, false)) {
            VendorSubscribeState.DISABLED
        } else {
            VendorSubscribeState.DENIED
        }
    }

    /**
     * 订阅渠道由 vivo 推送服务替应用创建，id 的具体格式没有文档，
     * 先按完全相同找，找不到再找包含渠道 id 的（类似小米的 mipush|包名|渠道id）。
     */
    private fun findSubscribeChannel(): NotificationChannel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null
        }
        val manager = ApplicationUtils.getApplication()
            .getSystemService(NotificationManager::class.java) ?: return null
        val channels = runCatching { manager.notificationChannels }.getOrNull() ?: return null
        val channel = channels.firstOrNull { it.id == SUBSCRIBE_CHANNEL_ID }
            ?: channels.firstOrNull { it.id.contains(SUBSCRIBE_CHANNEL_ID) }
        if (channel != null && channel.id != loggedChannelId) {
            loggedChannelId = channel.id
            WxpLogUtils.i(TAG, "找到订阅渠道，id=${channel.id}, importance=${channel.importance}")
        }
        return channel
    }

    /** 订阅弹窗只支持 Android 10 及以上，并且要 vpush 引擎支持；进程内缓存。 */
    private fun isSupported(): Boolean {
        supportCache?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            supportCache = false
            return false
        }
        // SDK 还没初始化等情况会抛异常，这次按不支持处理但不缓存，下次再查
        val supported = runCatching {
            PushClient.getInstance(ApplicationUtils.getApplication()).isPushSupport(SUPPORT_SUBSCRIBE_DIALOG_FLAG)
        }.onFailure {
            WxpLogUtils.w(TAG, "查询订阅弹窗能力失败", throwable = it)
        }.getOrNull() ?: return false
        supportCache = supported
        return supported
    }

    override fun getBannerText(): String {
        return when {
            currentState() == VendorSubscribeState.DISABLED -> "vivo订阅通知已关闭，收不到推送，点击开启"
            isLegacyCutoffPassed() -> "vivo订阅通知未开启，收不到推送，点击开启"
            else -> "vivo推送规则调整，请开启订阅通知，否则 4 月 1 日后收不到推送"
        }
    }

    /**
     * - 本地还没有订阅渠道：拉起 vivo 的授权弹窗；
     * - 本地有渠道但被关掉了：再调授权接口不会弹窗（6048），只能去系统设置里打开。
     */
    override fun showGuide(activity: Activity) {
        if (requesting || activity.isFinishing || activity.isDestroyed) {
            return
        }
        val channelExists = findSubscribeChannel() != null
        val message = if (channelExists) {
            "「$SUBSCRIBE_CHANNEL_NAME」已被关闭，关闭后收不到推送。\n\n请在接下来的设置页里打开「允许通知」开关。"
        } else {
            "vivo推送规则调整，需要开启「$SUBSCRIBE_CHANNEL_NAME」才能正常收到推送。\n\n接下来的系统弹窗里，请点「允许」。"
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle("开启vivo订阅通知")
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

    /** 拉起 vivo 授权弹窗。频控由 vivo 执行（6043 / 8111），用户的选择从 [onSubscribeDialogResult] 回来。 */
    private fun requestAuthorization(activity: Activity) {
        requesting = true
        runCatching {
            PushClient.getInstance(activity).showSubscribeDialog(activity, listOf(SUBSCRIBE_CHANNEL_ID)) { code, msg, displays ->
                ThreadUtils.runOnMainThread {
                    requesting = false
                    WxpLogUtils.i(TAG, "弹窗结果，code=$code, msg=$msg, displays=$displays")
                    onDialogResult(activity, code)
                }
            }
        }.onFailure {
            requesting = false
            WxpLogUtils.w(TAG, "调用授权弹窗失败", throwable = it)
        }
    }

    /** 弹窗是否拉起的结果，用户的选择不在这里。 */
    private fun onDialogResult(activity: Activity, code: Int) {
        if (code == CODE_SHOWN || code == CODE_DIALOG_SHOWING) {
            return
        }
        if (code == CODE_ANDROID_VERSION_NOT_SUPPORT || code == CODE_ENGINE_NOT_SUPPORT) {
            supportCache = false
        }
        PushChannelCoordinator.onVendorStateChanged()
        if (currentState() == VendorSubscribeState.GRANTED || activity.isFinishing || activity.isDestroyed) {
            return
        }
        when (code) {
            // 不在前台，等下次回到前台再提醒
            CODE_NOT_FOREGROUND -> return
            CODE_NOTIFICATION_OFF, CODE_NOTIFICATION_OFF_ENGINE -> {
                WxpToastUtils.showToast("请先打开应用的通知权限")
                PermissionUtils.gotoNotificationSettingPage()
                return
            }
            // 渠道都建好了但没开（关掉了，或者拒绝太多次），只能去设置里打开
            CODE_ALL_CHANNEL_CREATED -> {
                openChannelSettings(activity)
                return
            }
        }
        val message = when (code) {
            CODE_DIALOG_FREQUENCY, CODE_API_FREQUENCY -> "授权弹窗太频繁，请稍后再试，也可以切换到 WebSocket 推送。"
            CODE_ANDROID_VERSION_NOT_SUPPORT, CODE_ENGINE_NOT_SUPPORT -> "当前系统不支持vivo订阅通知，可以切换到 WebSocket 推送。"
            else -> "没有开启vivo订阅通知，可能收不到推送。可以稍后再试，也可以切换到 WebSocket 推送。"
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("未开启vivo订阅通知")
            .setMessage(message)
            .setPositiveButton("切换推送通道") { d, _ ->
                d.dismiss()
                WxpJumpPageUtils.jumpToPushChannelSetting(activity)
            }
            .setNegativeButton("知道了") { d, _ -> d.dismiss() }
            .create()
        DialogManager.show(activity, dialog)
    }

    /**
     * 用户在 vivo 弹窗里的选择，由 [VIVOClientPushMessageReceiver] 转过来。
     * 以本地渠道的实际开关为准，这里只是重新检测和上报的时机。
     */
    fun onSubscribeDialogResult(operateResult: Int, operations: List<ChannelOperation>?) {
        ThreadUtils.runOnMainThread {
            WxpLogUtils.i(TAG, "用户操作结果，operateResult=$operateResult, operations=$operations")
            PushChannelCoordinator.onVendorStateChanged()
            if (currentState() == VendorSubscribeState.GRANTED) {
                WxpToastUtils.showToast("已开启vivo订阅通知")
            } else if (operations?.any { it.operation == ChannelOperation.OPERATION_USER_REJECT || it.operation == ChannelOperation.OPERATION_REJECT_THRESHOLD } == true) {
                WxpToastUtils.showToast("未开启vivo订阅通知，可能收不到推送，可在「推送通道设置」里重新开启")
            }
        }
    }

    override fun openChannelSettings(activity: Activity) {
        val channelId = findSubscribeChannel()?.id
        if (channelId == null) {
            PermissionUtils.gotoNotificationSettingPage()
            return
        }
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        runCatching { activity.startActivity(intent) }.onFailure {
            WxpLogUtils.w(TAG, "打开渠道设置页失败", throwable = it)
            PermissionUtils.gotoNotificationSettingPage()
        }
    }
}
