package com.smjcco.wxpusher.page.pushchannel.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.push.PushChannel
import com.smjcco.wxpusher.push.PushChannelCoordinator
import com.smjcco.wxpusher.push.PushChannelSnapshot
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

/**
 * 厂商系统推送的铃声设置引导。
 *
 * Android 的通知类别创建后，声音由用户在系统设置中控制。本页只读取已验证的类别并引导
 * 用户跳转，绝不会删除、重建或修改任何通知类别。
 */
class SystemPushSoundGuideActivity : WxpBaseActivity() {
    private lateinit var pushStatus: TextView
    private lateinit var channelSection: View
    private lateinit var channelName: TextView
    private lateinit var channelSound: TextView
    private lateinit var directHint: TextView
    private lateinit var openChannelSettingButton: MaterialButton
    private lateinit var openAppNotificationSettingButton: MaterialButton
    private lateinit var manualGuide: TextView
    private lateinit var videoGuideButton: MaterialButton

    private var directChannel: NotificationChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_push_sound_guide)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "系统推送铃声设置"
        bindViews()
        bindActions()
    }

    override fun onResume() {
        super.onResume()
        // 用户从系统设置返回后，重新读取铃声和通知类别状态。
        render(PushChannelCoordinator.getSnapshot())
    }

    private fun bindViews() {
        pushStatus = findViewById(R.id.tv_system_push_status)
        channelSection = findViewById(R.id.layout_channel_section)
        channelName = findViewById(R.id.tv_channel_name)
        channelSound = findViewById(R.id.tv_channel_sound)
        directHint = findViewById(R.id.tv_direct_hint)
        openChannelSettingButton = findViewById(R.id.btn_open_channel_setting)
        openAppNotificationSettingButton = findViewById(R.id.btn_open_app_notification_setting)
        manualGuide = findViewById(R.id.tv_manual_guide)
        videoGuideButton = findViewById(R.id.btn_video_guide)
    }

    private fun bindActions() {
        openChannelSettingButton.setOnClickListener {
            directChannel?.let { channel ->
                WxpJumpPageUtils.jumpToSystemNotificationChannelSettings(channel.id, this)
            }
        }
        openAppNotificationSettingButton.setOnClickListener {
            WxpJumpPageUtils.jumpToSystemNotificationSettingPage(this)
        }
        videoGuideButton.setOnClickListener {
            val platform = PushChannelCoordinator.getSnapshot().vendorPlatform
            WxpJumpPageUtils.jumpToWebUrl(getGuidePageUrl(platform), this)
        }
    }

    private fun render(snapshot: PushChannelSnapshot) {
        val vendorEffective = snapshot.effectiveChannel == PushChannel.VENDOR
        if (!vendorEffective) {
            directChannel = null
            pushStatus.text = "当前实际使用 ${snapshot.effectiveName}，系统推送铃声暂不能设置。"
            channelSection.visibility = View.GONE
            directHint.text = "请先在上一页切换至系统推送，并等待切换成功后再设置铃声。"
            setDirectHintSecondary(false)
            openChannelSettingButton.visibility = View.GONE
            setSystemSettingsEnabled(false)
            manualGuide.text = "系统推送未生效时，修改系统通知铃声不会影响当前消息提醒。"
            videoGuideButton.visibility = if (hasGuideVideo(snapshot.vendorPlatform)) {
                View.VISIBLE
            } else {
                View.GONE
            }
            return
        }

        pushStatus.text = "当前使用${snapshot.effectiveName}，铃声由手机系统控制。"
        directChannel = findVerifiedDirectChannel(snapshot.vendorPlatform)
        val channel = directChannel
        channelSection.visibility = if (channel == null) View.GONE else View.VISIBLE
        openChannelSettingButton.visibility = if (channel == null) View.GONE else View.VISIBLE
        setSystemSettingsEnabled(true)

        if (channel != null) {
            channelName.text = channel.name
            channelSound.text = getChannelSoundDescription(channel)
            directHint.text = "已找到系统消息类别，可直接前往修改铃声。"
            setDirectHintSecondary(true)
        } else {
            directHint.text = getDirectUnavailableHint(snapshot.vendorPlatform)
            setDirectHintSecondary(false)
        }
        manualGuide.text = "打开手机系统设置，选择应用，找到“WxPusher”，选择 WxPusher 的“订阅消息”或“消息通知”，再选择“声音”或者“铃声”。"
        videoGuideButton.visibility = if (hasGuideVideo(snapshot.vendorPlatform)) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setSystemSettingsEnabled(enabled: Boolean) {
        openAppNotificationSettingButton.isEnabled = enabled
        openAppNotificationSettingButton.alpha = if (enabled) 1f else 0.45f
    }

    /** 成功识别后的补充说明弱化一级，其余引导仍使用正文颜色。 */
    private fun setDirectHintSecondary(secondary: Boolean) {
        val colorRes = if (secondary) {
            R.color.text_fit_theme_second
        } else {
            R.color.text_fit_theme_first
        }
        directHint.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    /**
     * 仅返回经项目验证、确实属于当前厂商系统推送的类别。
     * 不能猜测华为、荣耀、vivo、OPPO 或魅族的类别 ID，以免把保活或 WS 通知带到错误页面。
     */
    private fun findVerifiedDirectChannel(platform: DevicePlatform): NotificationChannel? {
        val channelId = when (platform) {
            DevicePlatform.Android_XIAOMI -> "mipush|$packageName|135072"
            else -> return null
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.getNotificationChannel(channelId)
    }

    private fun getChannelSoundDescription(channel: NotificationChannel): String {
        val soundUri = channel.sound ?: return "静音"
        val ringtoneName = runCatching {
            RingtoneManager.getRingtone(this, soundUri)?.getTitle(this)
        }.getOrNull()
        return if (ringtoneName.isNullOrEmpty()) {
            "已设置系统铃声"
        } else {
            ringtoneName
        }
    }

    private fun getDirectUnavailableHint(platform: DevicePlatform): String = when (platform) {
        DevicePlatform.Android_XIAOMI ->
            "暂未找到“订阅消息”类别。请先接收一条系统推送消息（可以点击右上角的测试发送一个消息），再返回此页设置铃声。"

        DevicePlatform.Android_MEIZU ->
            "魅族系统推送无法修改通知提醒铃声，如需修改提醒铃声，可切换成WxPusher自建通道。"

        else -> "当前手机品牌未识别到可直达系统的消息类别，请通过系统通知设置或视频教程修改铃声。"
    }

    private fun hasGuideVideo(platform: DevicePlatform): Boolean = when (platform) {
        DevicePlatform.Android_XIAOMI,
        DevicePlatform.Android_HUAWEI,
        DevicePlatform.Android_VIVO,
        DevicePlatform.Android_HONOR -> true

        else -> false
    }

    private fun getGuidePageUrl(platform: DevicePlatform): String =
        "https://wxpusher.zjiecode.com/docs/open-app-note/index.html?brand=${platform.getPlatform()}"

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_push_channel_setting, menu)
        return true
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SystemPushSoundGuideActivity::class.java))
        }
    }
}
