package com.smjcco.wxpusher.page.pushchannel.alert

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.dialog.ActionSheetDialogFragment
import com.smjcco.wxpusher.dialog.ActionSheetItem
import com.smjcco.wxpusher.push.ws.alert.WsAlertPlayer
import com.smjcco.wxpusher.push.ws.alert.WsAlertStore
import com.smjcco.wxpusher.push.ws.alert.WsAlertTones

/**
 * WS 通道的提醒方式设置页。
 *
 * 纯本地设置，改一项存一项，所以没有「保存」按钮，也没有加载失败态——这点和 iOS 的
 * 提醒铃声页不同，iOS 的铃声要存服务端。
 *
 * 提醒时长是这一页的主控件：拖到 0 就完全不额外提醒，下面几个开关也随之失效。
 */
class WsAlertSettingActivity : WxpBaseActivity() {
    private lateinit var durationSlider: Slider
    private lateinit var durationDes: TextView
    private lateinit var alertSectionHeader: TextView
    private lateinit var alertGroup: ViewGroup
    private lateinit var vibrateSwitch: SwitchMaterial
    private lateinit var torchSwitch: SwitchMaterial
    private lateinit var soundSwitch: SwitchMaterial
    private lateinit var forceLoudSwitch: SwitchMaterial
    private lateinit var toneRow: View
    private lateinit var toneValue: TextView
    private lateinit var tryButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ws_alert_setting)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "提醒方式设置"
        bindViews()
        bindActions()
        render()
    }

    override fun onPause() {
        // 试听最长能有 60 秒，跟着用户离开页面继续响是不能接受的
        WsAlertPlayer.stopAll()
        super.onPause()
    }

    private fun bindViews() {
        durationSlider = findViewById(R.id.slider_duration)
        durationDes = findViewById(R.id.tv_duration_des)
        alertSectionHeader = findViewById(R.id.tv_alert_section_header)
        alertGroup = findViewById(R.id.layout_alert_group)
        vibrateSwitch = findViewById(R.id.switch_vibrate)
        torchSwitch = findViewById(R.id.switch_torch)
        soundSwitch = findViewById(R.id.switch_sound)
        forceLoudSwitch = findViewById(R.id.switch_force_loud)
        toneRow = findViewById(R.id.layout_tone)
        toneValue = findViewById(R.id.tv_tone_value)
        tryButton = findViewById(R.id.btn_try)
    }

    private fun bindActions() {
        durationSlider.valueFrom = 0f
        durationSlider.valueTo = WsAlertStore.DURATION_MAX.toFloat()
        durationSlider.stepSize = WsAlertStore.DURATION_STEP.toFloat()
        durationSlider.setLabelFormatter { "${it.toInt()} 秒" }
        // 拖动过程中只刷新界面，松手才落盘，避免一次拖动写几十次 SharedPreferences
        durationSlider.addOnChangeListener { _, value, _ ->
            renderDuration(value.toInt())
        }
        durationSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit

            override fun onStopTrackingTouch(slider: Slider) {
                WsAlertStore.setDurationSeconds(slider.value.toInt())
                render()
            }
        })

        findViewById<View>(R.id.layout_vibrate).setOnClickListener {
            WsAlertStore.setVibrateEnabled(!WsAlertStore.isVibrateEnabled())
            render()
        }
        findViewById<View>(R.id.layout_torch).setOnClickListener {
            WsAlertStore.setTorchEnabled(!WsAlertStore.isTorchEnabled())
            render()
        }
        findViewById<View>(R.id.layout_sound).setOnClickListener {
            WsAlertStore.setSoundEnabled(!WsAlertStore.isSoundEnabled())
            render()
        }
        findViewById<View>(R.id.layout_force_loud).setOnClickListener {
            WsAlertStore.setForceLoud(!WsAlertStore.isForceLoud())
            render()
        }
        toneRow.setOnClickListener { showToneChooser() }
        tryButton.setOnClickListener { WsAlertPlayer.alertOnce() }
    }

    /** 选中即试听，和 iOS 的提醒铃声页一致。 */
    private fun showToneChooser() {
        val currentKey = WsAlertStore.getSoundKey()
        val items = WsAlertTones.all.map { option ->
            val label = if (option.key == currentKey) {
                "${option.name} ✓"
            } else {
                option.name
            }
            ActionSheetItem(label) {
                WsAlertStore.setSoundKey(option.key)
                render()
                WsAlertPlayer.previewTone(option.key)
            }
        }
        ActionSheetDialogFragment(listOf(items)).show(supportFragmentManager, "wsAlertTone")
    }

    private fun render() {
        renderDuration(WsAlertStore.getDurationSeconds())
        durationSlider.value = WsAlertStore.getDurationSeconds().toFloat()
        vibrateSwitch.isChecked = WsAlertStore.isVibrateEnabled()
        torchSwitch.isChecked = WsAlertStore.isTorchEnabled()
        soundSwitch.isChecked = WsAlertStore.isSoundEnabled()
        forceLoudSwitch.isChecked = WsAlertStore.isForceLoud()
        toneValue.text = WsAlertTones.find(WsAlertStore.getSoundKey()).name

        // 时长为 0 时 App 不做任何额外提醒，下面几个开关也就没有意义了
        val alertEnabled = WsAlertStore.getDurationSeconds() > 0
        setRowGroupEnabled(alertEnabled)
        // 响铃关掉时，提示音和「静音时也响铃」跟着失效
        val soundEnabled = alertEnabled && WsAlertStore.isSoundEnabled()
        setRowEnabled(toneRow, soundEnabled)
        setRowEnabled(findViewById(R.id.layout_force_loud), soundEnabled)
    }

    private fun renderDuration(seconds: Int) {
        durationDes.text = if (seconds <= 0) {
            "不额外提醒，按系统设置在通知栏静默显示"
        } else {
            "收到消息后持续提醒 $seconds 秒"
        }
    }

    private fun setRowGroupEnabled(enabled: Boolean) {
        alertSectionHeader.text = if (enabled) {
            "提醒方式"
        } else {
            "提醒方式（把提醒时长拖到 0 以上后可用）"
        }
        for (index in 0 until alertGroup.childCount) {
            setRowEnabled(alertGroup.getChildAt(index), enabled)
        }
        setRowEnabled(tryButton, enabled)
    }

    private fun setRowEnabled(row: View, enabled: Boolean) {
        row.isEnabled = enabled
        row.alpha = if (enabled) {
            1f
        } else {
            0.45f
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, WsAlertSettingActivity::class.java))
        }
    }
}
