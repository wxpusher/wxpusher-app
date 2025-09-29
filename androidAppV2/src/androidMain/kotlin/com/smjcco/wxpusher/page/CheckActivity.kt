package com.smjcco.wxpusher.page

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxPusherUtils

class CheckActivity : ComponentActivity() {
    var permissionRequester: PermissionRequester? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.check_activity)
        enableEdgeToEdge()
        setupNotificationPermissionItem()
        setupNotificationSettingsItem()
        setupAutoStartItem()
        setupBackgroundRunningItem()

        updateStatus()
    }

    private fun updateStatus() {
        //发送通知的权限
        val permissionItemView = findViewById<View>(R.id.notification_permission_item)
        updateNotificationPermissionStatus(
            permissionItemView,
            PermissionUtils.hasNotificationPermission(this)
        )

        //铃声震动和提醒
        val noteItemView = findViewById<View>(R.id.notification_settings_item)
        updateNotificationPermissionStatus(
            noteItemView,
            areNotificationSettingsEnabled()
        )
    }

    private fun setupNotificationPermissionItem() {
        val itemView = findViewById<View>(R.id.notification_permission_item)
        val titleView = itemView.findViewById<TextView>(R.id.check_title)
        val descriptionView = itemView.findViewById<TextView>(R.id.check_description)

        titleView.text = getString(R.string.check_notification_permission)
        descriptionView.text = getString(R.string.check_notification_permission_desc)
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "android.permission.POST_NOTIFICATIONS"
        permissionRequester = PermissionRequester(
            this, permission,
            "需要发送通知权限",
            "WxPusher是一个消息推送平台，当有新消息到达的时候，我们会第一时间给你发送通知，因此需要你授予发送通知的权限，否则我们无法发送消息通知，你可能会因此遗漏消息，是否授予权限？",
            "缺少通知权限",
            "本应用核心功能是发送消息通知，缺少通知权限会导致你遗漏消息。\n\n打开方式：点击“去设置”-“通知管理”-打开允许通知"
        ) {

            PermissionUtils.gotoNotificationSettingPage()
        }
        itemView.setOnClickListener {
            permissionRequester?.request() {
                if (it) {
                    WxPusherUtils.toast("已经有发送通知的权限")
                }
            }
        }
    }

    private fun updateNotificationPermissionStatus(itemView: View, status: Boolean) {
        val statusIndicator = itemView.findViewById<View>(R.id.status_indicator)
        val statusText = itemView.findViewById<TextView>(R.id.status_text)
        statusIndicator.setBackgroundResource(
            if (status) R.drawable.status_indicator_success else R.drawable.status_indicator_error
        )

        statusText.text = getString(
            if (status) R.string.check_status_enabled else R.string.check_status_disabled
        )

        statusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (status) R.color.check_success else R.color.check_error
            )
        )
    }


    private fun setupNotificationSettingsItem() {
        val itemView = findViewById<View>(R.id.notification_settings_item)
        val titleView = itemView.findViewById<TextView>(R.id.check_title)
        val descriptionView = itemView.findViewById<TextView>(R.id.check_description)

        titleView.text = getString(R.string.check_notification_settings)
        descriptionView.text = getString(R.string.check_notification_settings_desc)
        itemView.setOnClickListener {
//            PermissionUtils.openNotificationChannelSettings()
        }
    }

    private fun areNotificationSettingsEnabled(): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 检查通知是否已启用
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return false
        }

        return notificationManager.notificationChannels.find { it.importance != NotificationManager.IMPORTANCE_NONE } != null
    }


    private fun setupAutoStartItem() {
        val itemView = findViewById<View>(R.id.autostart_item)
        val titleView = itemView.findViewById<TextView>(R.id.check_title)
        val descriptionView = itemView.findViewById<TextView>(R.id.check_description)
        val statusCon = itemView.findViewById<View>(R.id.status_con)
        statusCon.visibility = View.GONE
        titleView.text = getString(R.string.check_autostart)
        descriptionView.text = getString(R.string.check_autostart_desc)

        itemView.setOnClickListener {
            openAutoStartSettings()
        }
    }

    private fun openAutoStartSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()

        try {
            val intent = Intent()

            when {
                // 小米
                manufacturer.contains("xiaomi") -> {
                    intent.component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
                // 华为
                manufacturer.contains("huawei") -> {
                    intent.component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
                // OPPO
                manufacturer.contains("oppo") -> {
                    intent.component = android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
                // VIVO
                manufacturer.contains("vivo") -> {
                    intent.component = android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
                // 其他品牌
                else -> {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts(Settings.EXTRA_APP_PACKAGE, packageName, null)
                }
            }

            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果无法打开特定页面，则打开应用详情页
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    private fun setupBackgroundRunningItem() {
        val itemView = findViewById<View>(R.id.background_running_item)
        val titleView = itemView.findViewById<TextView>(R.id.check_title)
        val descriptionView = itemView.findViewById<TextView>(R.id.check_description)

        titleView.text = getString(R.string.check_background_running)
        descriptionView.text = getString(R.string.check_background_running_desc)
        val statusCon = itemView.findViewById<View>(R.id.status_con)
        statusCon.visibility = View.GONE

        itemView.setOnClickListener {
            openBatteryOptimizationSettings()
        }
    }


    private fun openBatteryOptimizationSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()

        try {
            val intent = Intent()

            when {
                // 标准Android设置
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                }
                // 小米
                manufacturer.contains("xiaomi") -> {
                    intent.component = android.content.ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                    )
                }
                // 华为
                manufacturer.contains("huawei") -> {
                    intent.component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
                // OPPO
                manufacturer.contains("oppo") -> {
                    intent.component = android.content.ComponentName(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                    )
                }
                // VIVO
                manufacturer.contains("vivo") -> {
                    intent.component = android.content.ComponentName(
                        "com.vivo.abe",
                        "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
                    )
                }
                // 其他品牌
                else -> {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", packageName, null)
                }
            }

            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开特定页面，则打开电池优化设置页面
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                // 如果还是无法打开，则打开应用详情页
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 页面恢复时更新所有状态
        updateStatus()
    }
}