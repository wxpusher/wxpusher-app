package com.smjcco.wxpusher.page.pushchannel

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.push.PushChannel
import com.smjcco.wxpusher.push.PushChannelCoordinator
import com.smjcco.wxpusher.push.PushChannelSnapshot
import com.smjcco.wxpusher.push.PushPlatformResolver
import com.smjcco.wxpusher.push.VendorAvailability
import com.smjcco.wxpusher.push.ws.alert.WsAlertStore
import com.smjcco.wxpusher.push.ws.connect.WsManager
import com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveService
import com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveServiceStarter
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

/**
 * 当前设备的推送通道设置页。
 *
 * 页面只负责展示状态和接收用户操作，实际切换、持久化与降级策略由
 * [PushChannelCoordinator] 统一处理。
 */
class PushChannelSettingActivity : WxpBaseActivity() {
    private lateinit var vendorCard: MaterialCardView
    private lateinit var vendorTitle: TextView
    private lateinit var vendorState: TextView
    private lateinit var vendorRadio: RadioButton
    private lateinit var retryVendor: TextView
    private lateinit var vendorAlertSetting: View
    private lateinit var vendorAlertSummary: TextView
    private lateinit var wsCard: MaterialCardView
    private lateinit var wsState: TextView
    private lateinit var wsRadio: RadioButton
    private lateinit var wsAlertSetting: View
    private lateinit var wsAlertSummary: TextView
    private lateinit var wsDependencySection: View
    private lateinit var wsDependencySummary: TextView
    private lateinit var foregroundNotificationCheck: DependencyCheckViews
    private lateinit var autoStartCheck: DependencyCheckViews
    private lateinit var backgroundCheck: DependencyCheckViews
    private lateinit var foregroundNotificationRequester: PermissionRequester
    private var lastErrorMessage: String? = null
    private var latestSnapshot: PushChannelSnapshot? = null
    private var wsDependencyActionsEnabled = false
    private var waitingForAutoStartConfirmation = false

    private data class DependencyCheckViews(
        val row: View,
        val icon: ImageView,
        val status: TextView,
    )

    // 通道状态变化时刷新卡片选中、可用和错误状态。
    private val channelListener: (PushChannelSnapshot) -> Unit = { render(it) }

    // WS 连接状态独立变化，不能只依赖通道快照刷新。
    private val wsConnectListener = object : WsManager.IWsConnectChangedListener {
        override fun onChanged(connectStatus: WsManager.WsConnectStatus) {
            renderWsState(connectStatus)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_channel_setting)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "推送通道设置"
        bindViews()
        setupForegroundNotificationRequester()
        bindActions()
    }

    override fun onStart() {
        super.onStart()
        PushChannelCoordinator.addListener(channelListener)
        WsManager.addConnectChangedListener(wsConnectListener)
    }

    override fun onResume() {
        super.onResume()
        // 从提醒方式设置页返回后刷新摘要，同时维持“当前实际通道”决定入口可用性的规则。
        val snapshot = latestSnapshot ?: PushChannelCoordinator.getSnapshot()
        renderWsAlertSetting(snapshot)
        renderWsDependencyChecks(snapshot)
        if (waitingForAutoStartConfirmation) {
            waitingForAutoStartConfirmation = false
            showAutoStartConfirmationDialog()
        }
    }

    override fun onStop() {
        WsManager.removeConnectChangedListener(wsConnectListener)
        PushChannelCoordinator.removeListener(channelListener)
        super.onStop()
    }

    private fun bindViews() {
        vendorCard = findViewById(R.id.card_vendor_push)
        vendorTitle = findViewById(R.id.tv_vendor_title)
        vendorState = findViewById(R.id.tv_vendor_state)
        vendorRadio = findViewById(R.id.radio_vendor)
        retryVendor = findViewById(R.id.btn_retry_vendor)
        vendorAlertSetting = findViewById(R.id.layout_vendor_alert_setting)
        vendorAlertSummary = findViewById(R.id.tv_vendor_alert_summary)
        wsCard = findViewById(R.id.card_ws_push)
        wsState = findViewById(R.id.tv_ws_state)
        wsRadio = findViewById(R.id.radio_ws)
        wsAlertSetting = findViewById(R.id.layout_ws_alert_setting)
        wsAlertSummary = findViewById(R.id.tv_ws_alert_summary)
        wsDependencySection = findViewById(R.id.layout_ws_dependency_section)
        wsDependencySummary = findViewById(R.id.tv_ws_dependency_summary)
        foregroundNotificationCheck = DependencyCheckViews(
            row = findViewById(R.id.layout_ws_foreground_notification_check),
            icon = findViewById(R.id.iv_ws_foreground_notification_status),
            status = findViewById(R.id.tv_ws_foreground_notification_status),
        )
        autoStartCheck = DependencyCheckViews(
            row = findViewById(R.id.layout_ws_auto_start_check),
            icon = findViewById(R.id.iv_ws_auto_start_status),
            status = findViewById(R.id.tv_ws_auto_start_status),
        )
        backgroundCheck = DependencyCheckViews(
            row = findViewById(R.id.layout_ws_background_check),
            icon = findViewById(R.id.iv_ws_background_status),
            status = findViewById(R.id.tv_ws_background_status),
        )
    }

    private fun setupForegroundNotificationRequester() {
        foregroundNotificationRequester = PermissionRequester(
            activity = this,
            permission = Manifest.permission.POST_NOTIFICATIONS,
            explainTitle = "需要保活前台通知权限",
            explainMessage = "自建链接需要显示一条常驻通知，才能尽量保持后台连接。",
            guideTitle = "开启保活前台通知权限",
            guideMessage = "请在系统通知设置中允许 WxPusher 显示通知。",
            gotoSetting = { WxpJumpPageUtils.jumpToSystemNotificationSettingPage(this) },
        )
    }

    private fun bindActions() {
        vendorCard.setOnClickListener { PushChannelCoordinator.selectVendor() }
        wsCard.setOnClickListener {
            PushChannelCoordinator.selectWebSocket()
        }
        retryVendor.setOnClickListener { PushChannelCoordinator.retryVendorRegistration() }
        // 系统推送铃声只能由系统通知设置修改。入口是否可用由当前“实际生效”的
        // 通道决定，不能用用户偏好判断，否则厂商通道还未切换成功时会误导用户。
        vendorAlertSetting.setOnClickListener {
            if (vendorAlertSetting.isEnabled) {
                WxpJumpPageUtils.jumpToSystemPushSoundGuide(this)
            }
        }
        // 子 view 自己消费点击，不会连带触发卡片的「选中 WS 通道」。
        // 仅在 WS 已实际生效时允许打开，避免用户误以为它能影响厂商系统推送。
        wsAlertSetting.setOnClickListener {
            if (wsAlertSetting.isEnabled) {
                WxpJumpPageUtils.jumpToWsAlertSetting(this)
            }
        }
        foregroundNotificationCheck.row.setOnClickListener {
            if (wsDependencyActionsEnabled) {
                handleForegroundNotificationCheck()
            }
        }
        autoStartCheck.row.setOnClickListener {
            if (wsDependencyActionsEnabled) {
                handleAutoStartCheck()
            }
        }
        backgroundCheck.row.setOnClickListener {
            if (wsDependencyActionsEnabled) {
                handleBackgroundCheck()
            }
        }
    }

    /** 根据协调器快照完整刷新两个通道卡片。 */
    private fun render(snapshot: PushChannelSnapshot) {
        latestSnapshot = snapshot
        vendorTitle.text = snapshot.vendorName
        vendorState.text = getVendorState(snapshot.vendorAvailability)
        vendorRadio.isChecked = snapshot.effectiveChannel == PushChannel.VENDOR
        wsRadio.isChecked = snapshot.effectiveChannel == PushChannel.WEBSOCKET
        renderWsState()

        val vendorEnabled = snapshot.vendorAvailability == VendorAvailability.READY
            && !snapshot.switching
        vendorCard.isEnabled = vendorEnabled
        vendorCard.alpha = if (vendorEnabled) {
            1f
        } else {
            0.55f
        }
        wsCard.isEnabled = !snapshot.switching
        wsCard.alpha = if (snapshot.switching) {
            0.7f
        } else {
            1f
        }
        retryVendor.visibility = if (
            snapshot.vendorAvailability == VendorAvailability.REGISTER_FAILED && !snapshot.switching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        renderVendorAlertSetting(snapshot)
        renderWsAlertSetting(snapshot)
        renderWsDependencyChecks(snapshot)

        val selectedColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val normalColor = ContextCompat.getColor(this, R.color.input_border_color)
        vendorCard.strokeColor = if (vendorRadio.isChecked) {
            selectedColor
        } else {
            normalColor
        }
        vendorCard.strokeWidth = if (vendorRadio.isChecked) {
            2
        } else {
            1
        }
        wsCard.strokeColor = if (wsRadio.isChecked) {
            selectedColor
        } else {
            normalColor
        }
        wsCard.strokeWidth = if (wsRadio.isChecked) {
            2
        } else {
            1
        }
        vendorRadio.buttonTintList = ColorStateList.valueOf(selectedColor)
        wsRadio.buttonTintList = ColorStateList.valueOf(selectedColor)

        snapshot.errorMessage?.let {
            if (it != lastErrorMessage) {
                WxpToastUtils.showToast(it)
                lastErrorMessage = it
            }
        }
        if (snapshot.errorMessage == null) {
            lastErrorMessage = null
        }
    }

    /**
     * 与 WS 的本地提醒设置不同，厂商推送的声音归系统通知类别所有。
     * 只有厂商推送已经真正生效时才允许进入，避免用户把它误认为 WS 提醒设置。
     */
    private fun renderVendorAlertSetting(snapshot: PushChannelSnapshot) {
        val vendorEffective = snapshot.effectiveChannel == PushChannel.VENDOR
        vendorAlertSetting.isEnabled = vendorEffective
        vendorAlertSetting.alpha = if (vendorEffective) 1f else 0.45f
        vendorAlertSummary.text = if (vendorEffective) {
            "去系统设置铃声"
        } else if (snapshot.preference == com.smjcco.wxpusher.push.PushChannelPreference.VENDOR) {
            "系统推送生效后可设置"
        } else {
            "切换至系统推送后可设置"
        }
    }

    /**
     * WS 提醒由 App 本地执行，只在自建链接已经实际生效时才允许修改。
     * 这与厂商系统推送铃声入口使用相同的可用性判断，均以实际通道而非用户偏好为准。
     */
    private fun renderWsAlertSetting(snapshot: PushChannelSnapshot) {
        val wsEffective = snapshot.effectiveChannel == PushChannel.WEBSOCKET
        wsAlertSetting.isEnabled = wsEffective
        wsAlertSetting.alpha = if (wsEffective) 1f else 0.45f
        wsAlertSummary.text = if (wsEffective) {
            WsAlertStore.summary()
        } else {
            "切换至自建链接可设置"
        }
    }

    /**
     * 长连接运行保障只对已经实际生效的 WS 通道有意义。
     * 通知和后台限制可以读取系统状态；自启动没有公开查询 API，只展示用户确认状态。
     */
    private fun renderWsDependencyChecks(snapshot: PushChannelSnapshot) {
        val wsEffective = snapshot.effectiveChannel == PushChannel.WEBSOCKET
        wsDependencyActionsEnabled = wsEffective
        wsDependencySection.visibility = if (wsEffective) View.VISIBLE else View.GONE
        if (!wsEffective) {
            return
        }

        val notificationReady = isKeepAliveNotificationReady()
        val autoStartConfirmed = WxpSaveService.get(KEY_WS_AUTO_START_CONFIRMED, false)
        val backgroundReady = DeviceUtils.canRunInBackgroundWithoutBatteryRestrictions()
        renderDependencyStatus(foregroundNotificationCheck, notificationReady, "已开启")
        renderDependencyStatus(autoStartCheck, autoStartConfirmed, "已确认")
        renderDependencyStatus(backgroundCheck, backgroundReady, "已开启")

        val completedCount = listOf(
            notificationReady,
            autoStartConfirmed,
            backgroundReady,
        ).count { it }
        wsDependencySummary.text = if (completedCount == 3) {
            "3/3 已完成"
        } else {
            "还需设置 ${3 - completedCount} 项"
        }
        wsDependencySummary.setTextColor(
            ContextCompat.getColor(
                this,
                if (completedCount == 3) R.color.check_success else R.color.check_error,
            ),
        )
    }

    private fun renderDependencyStatus(
        views: DependencyCheckViews,
        ready: Boolean,
        readyText: String,
    ) {
        val color = ContextCompat.getColor(
            this,
            if (ready) R.color.check_success else R.color.check_error,
        )
        views.icon.setImageResource(if (ready) R.drawable.ic_done else R.drawable.ic_warning)
        views.icon.imageTintList = ColorStateList.valueOf(color)
        views.status.text = if (ready) readyText else "请设置"
        views.status.setTextColor(color)
        views.icon.contentDescription = views.status.text
    }

    private fun isKeepAliveNotificationReady(): Boolean {
        if (!PermissionUtils.hasNotificationPermission(this) ||
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
        ) {
            return false
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(
            KeepWsAliveService.KeepWsAliveNotificationChannelId,
        )
        // 首次启动服务前通道还不存在；只要应用通知权限可用，服务创建通道后即可展示。
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun handleForegroundNotificationCheck() {
        if (!PermissionUtils.hasNotificationPermission(this)) {
            foregroundNotificationRequester.request { granted ->
                if (granted && PermissionUtils.hasNotificationPermission(this)) {
                    KeepWsAliveServiceStarter.start(this)
                }
                renderWsDependencyChecks(latestSnapshot ?: PushChannelCoordinator.getSnapshot())
            }
            return
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            WxpToastUtils.showToast("请在系统设置中允许 WxPusher 显示通知")
            WxpJumpPageUtils.jumpToSystemNotificationSettingPage(this)
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(
            KeepWsAliveService.KeepWsAliveNotificationChannelId,
        )
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
            WxpJumpPageUtils.jumpToSystemNotificationChannelSettings(
                channelId = channel.id,
                activity = this,
                soundOnly = false,
            )
        } else {
            WxpToastUtils.showToast("保活前台通知权限已开启")
        }
    }

    private fun handleAutoStartCheck() {
        waitingForAutoStartConfirmation = true
        if (!WxpJumpPageUtils.jumpToSystemAutoStartSettings(this)) {
            waitingForAutoStartConfirmation = false
            showManualAutoStartGuide()
        }
    }

    /** 厂商没有可用直达入口时，展示可操作的手动路径，不把“无法检测”误报为无权限。 */
    private fun showManualAutoStartGuide() {
        val snapshot = latestSnapshot ?: PushChannelCoordinator.getSnapshot()
        val dialog = AlertDialog.Builder(this)
            .setTitle("手动开启自启动")
            .setMessage(getManualAutoStartGuide(snapshot.vendorPlatform))
            .setPositiveButton("打开应用详情") { _, _ ->
                waitingForAutoStartConfirmation = true
                WxpJumpPageUtils.jumpToSystemAppSettings(this)
            }
            .setNeutralButton("我已开启") { _, _ ->
                saveAutoStartConfirmation(true)
            }
            .setNegativeButton("取消", null)
            .create()
        DialogManager.show(this, dialog)
    }

    private fun getManualAutoStartGuide(platform: DevicePlatform): String {
        val path = when (platform) {
            DevicePlatform.Android_XIAOMI ->
                "设置 → 应用设置 → 授权管理 → 自启动管理，允许 WxPusher 自启动。"

            DevicePlatform.Android_HUAWEI, DevicePlatform.Android_HONOR ->
                "手机管家 → 应用启动管理 → WxPusher，关闭自动管理，并允许自启动和后台活动。"

            DevicePlatform.Android_VIVO ->
                "设置 → 应用与权限 → 权限管理 → 自启动，允许 WxPusher 自启动。"

            DevicePlatform.Android_OPPO ->
                "设置 → 应用 → 自启动或关联启动管理，允许 WxPusher 自启动和后台运行。"

            DevicePlatform.Android_MEIZU ->
                "手机管家 → 权限管理 → 后台管理，允许 WxPusher 后台运行和自启动。"

            else ->
                "请在系统设置或手机管家中找到应用、自启动或后台运行设置，允许 WxPusher 自启动。"
        }
        return "$path\n\n不同品牌不同系统版本的菜单名称可能略有不同。WxPusher 无法确定此项状态，请你自行前往手机系统进行设置。"
    }

    private fun showAutoStartConfirmationDialog() {
        if (latestSnapshot?.effectiveChannel != PushChannel.WEBSOCKET) {
            return
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("确认自启动设置")
            .setMessage("系统不提供自启动权限的状态查询。请确认你已经允许 WxPusher 自启动或后台启动。")
            .setPositiveButton("已允许") { _, _ -> saveAutoStartConfirmation(true) }
            .setNegativeButton("暂未允许") { _, _ -> saveAutoStartConfirmation(false) }
            .create()
        DialogManager.show(this, dialog)
    }

    private fun saveAutoStartConfirmation(confirmed: Boolean) {
        WxpSaveService.set(KEY_WS_AUTO_START_CONFIRMED, confirmed)
        renderWsDependencyChecks(latestSnapshot ?: PushChannelCoordinator.getSnapshot())
    }

    private fun handleBackgroundCheck() {
        if (DeviceUtils.canRunInBackgroundWithoutBatteryRestrictions()) {
            WxpToastUtils.showToast("后台运行和电量优化设置已就绪")
            return
        }
        if (!DeviceUtils.isIgnoringBatteryOptimizations()) {
            if(PushPlatformResolver.detectVendorPushPlatform()== DevicePlatform.Android_XIAOMI){
                WxpToastUtils.showToast("请选择：无限制")
            }else{
                WxpToastUtils.showToast("请允许 WxPusher 忽略电池优化（一直在后台运行）")
            }
            WxpJumpPageUtils.jumpToSystemIgnoreBatteryOptimizationSettings(this)
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("允许应用后台运行")
            .setMessage("系统已限制 WxPusher 在后台运行。请在应用详情的电池或后台运行设置中选择“允许后台运行”或“不限制”。")
            .setPositiveButton("打开应用详情") { _, _ ->
                WxpJumpPageUtils.jumpToSystemAppSettings(this)
            }
            .setNegativeButton("取消", null)
            .create()
        DialogManager.show(this, dialog)
    }

    /** 仅在 WS 实际生效时显示实时连接状态。 */
    private fun renderWsState(
        connectStatus: WsManager.WsConnectStatus = WsManager.getConnectStatus(),
    ) {
        if (latestSnapshot?.effectiveChannel != PushChannel.WEBSOCKET) {
            // 保留状态行占位，避免切换通道时卡片高度发生跳动。
            wsState.visibility = View.INVISIBLE
            return
        }
        wsState.visibility = View.VISIBLE
        wsState.text = when (connectStatus) {
            WsManager.WsConnectStatus.Connected -> "长连接状态：已连接"
            WsManager.WsConnectStatus.Connecting -> "长连接状态：连接中…"
            WsManager.WsConnectStatus.Closing -> "长连接状态：正在断开…"
            WsManager.WsConnectStatus.NotConnect -> "长连接状态：未连接"
        }
    }

    /** 将厂商注册状态转换为面向用户的说明。 */
    private fun getVendorState(availability: VendorAvailability): String = when (availability) {
        VendorAvailability.READY -> "系统级推送，更稳定、更省电"
        VendorAvailability.REGISTERING -> "正在注册系统推送…"
        VendorAvailability.UNSUPPORTED -> "当前手机不支持系统推送"
        VendorAvailability.REGISTER_FAILED -> "注册系统推送失败，当前暂不可用"
        VendorAvailability.CONFIG_DISABLED -> "系统已暂停当前厂商推送通道"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_push_channel_setting, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.menu_push_channel_test -> {
                WxpJumpPageUtils.jumpToWebUrl(
                    "${WxpConfig.appFeUrl}/app/#/send-test-guide",
                    this,
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val KEY_WS_AUTO_START_CONFIRMED = "WsKeepAlive_AutoStartConfirmed"

        fun start(context: Context) {
            context.startActivity(Intent(context, PushChannelSettingActivity::class.java))
        }
    }
}
