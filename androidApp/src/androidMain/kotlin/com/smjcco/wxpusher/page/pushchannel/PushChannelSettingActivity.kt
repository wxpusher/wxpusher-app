package com.smjcco.wxpusher.page.pushchannel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.push.PushChannel
import com.smjcco.wxpusher.push.PushChannelCoordinator
import com.smjcco.wxpusher.push.PushChannelSnapshot
import com.smjcco.wxpusher.push.VendorAvailability
import com.smjcco.wxpusher.push.ws.connect.WsManager
import com.smjcco.wxpusher.utils.DeviceUtils
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
    private lateinit var wsCard: MaterialCardView
    private lateinit var wsState: TextView
    private lateinit var wsRadio: RadioButton
    private var lastErrorMessage: String? = null
    private var latestSnapshot: PushChannelSnapshot? = null

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
        bindActions()
    }

    override fun onStart() {
        super.onStart()
        PushChannelCoordinator.addListener(channelListener)
        WsManager.addConnectChangedListener(wsConnectListener)
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
        wsCard = findViewById(R.id.card_ws_push)
        wsState = findViewById(R.id.tv_ws_state)
        wsRadio = findViewById(R.id.radio_ws)
    }

    private fun bindActions() {
        vendorCard.setOnClickListener { PushChannelCoordinator.selectVendor() }
        wsCard.setOnClickListener {
            PushChannelCoordinator.selectWebSocket()
            // WS 需要在后台保持连接，因此主动选择时复用首页的电量优化引导。
            if (!DeviceUtils.isIgnoringBatteryOptimizations()) {
                WxpToastUtils.showToast("请选择“不限制后台运行”或允许关闭电量优化")
                WxpJumpPageUtils.jumpToSystemIgnoreBatteryOptimizationSettings(this)
            }
        }
        retryVendor.setOnClickListener { PushChannelCoordinator.retryVendorRegistration() }
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
        fun start(context: Context) {
            context.startActivity(Intent(context, PushChannelSettingActivity::class.java))
        }
    }
}
