package com.smjcco.wxpusher.page

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.utils.ThreadUtils

class TestPanelActivity : ComponentActivity() {

    companion object {
        const val DEFAULT_HOST = "https://wxpusher.zjiecode.com"
        const val DEFAULT_APP_FE_URL = "https://wxpusher.zjiecode.com"
        const val DEFAULT_WS_URL = "wss://wxpusher.zjiecode.com"

        const val TEST_HOST = "http://wxpusher.test.zjiecode.com"
        const val TEST_APP_FE_URL = "http://wxpusher.test.zjiecode.com"
        const val TEST_WS_URL = "ws://wxpusher.test.zjiecode.com"
    }

    private lateinit var hostRadioGroup: RadioGroup
    private lateinit var hostCustom: RadioButton
    private lateinit var hostEditText: EditText

    private lateinit var webUrlRadioGroup: RadioGroup
    private lateinit var webUrlCustom: RadioButton
    private lateinit var webUrlEditText: EditText

    private lateinit var wsUrlRadioGroup: RadioGroup
    private lateinit var wsUrlCustom: RadioButton
    private lateinit var wsUrlEditText: EditText

    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_panel_activity)
        enableEdgeToEdge()

        initViews()
        loadSavedSettings()
        setupListeners()
    }

    private fun initViews() {
        hostRadioGroup = findViewById(R.id.host_radio_group)
        hostCustom = findViewById(R.id.host_custom)
        hostEditText = findViewById(R.id.host_edit_text)

        webUrlRadioGroup = findViewById(R.id.web_url_radio_group)
        webUrlCustom = findViewById(R.id.web_url_custom)
        webUrlEditText = findViewById(R.id.web_url_edit_text)

        wsUrlRadioGroup = findViewById(R.id.ws_url_radio_group)
        wsUrlCustom = findViewById(R.id.ws_url_custom)
        wsUrlEditText = findViewById(R.id.ws_url_edit_text)

        confirmButton = findViewById(R.id.confirm_button)
    }

    private fun loadSavedSettings() {
        // 加载保存的Host设置
        val savedHost = WxpConfig.baseUrl
        when (savedHost) {
            DEFAULT_HOST -> findViewById<RadioButton>(R.id.host_prod).isChecked = true
            TEST_HOST -> findViewById<RadioButton>(R.id.host_test).isChecked = true
            else -> {
                hostCustom.isChecked = true
                hostEditText.setText(savedHost)
            }
        }

        // 加载保存的WebUrl设置
        val savedWebUrl = WxpConfig.appFeUrl
        when (savedWebUrl) {
            DEFAULT_APP_FE_URL -> findViewById<RadioButton>(R.id.web_url_prod).isChecked = true
            TEST_APP_FE_URL -> findViewById<RadioButton>(R.id.web_url_test).isChecked = true
            else -> {
                webUrlCustom.isChecked = true
                webUrlEditText.setText(savedWebUrl)
            }
        }

        // 加载保存的WsUrl设置
        val savedWsUrl = WxpConfig.wsUrl
        when (savedWsUrl) {
            DEFAULT_WS_URL -> findViewById<RadioButton>(R.id.ws_url_prod).isChecked = true
            TEST_WS_URL -> findViewById<RadioButton>(R.id.ws_url_test).isChecked = true
            else -> {
                wsUrlCustom.isChecked = true
                wsUrlEditText.setText(savedWsUrl)
            }
        }

        // 设置EditText的可见性
        updateHostEditTextVisibility()
        updateWebUrlEditTextVisibility()
        updateWsUrlEditTextVisibility()
    }

    private fun setupListeners() {
        hostRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateHostEditTextVisibility()
        }

        webUrlRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateWebUrlEditTextVisibility()
        }

        wsUrlRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateWsUrlEditTextVisibility()
        }

        confirmButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun updateHostEditTextVisibility() {
        hostEditText.visibility = if (hostCustom.isChecked) View.VISIBLE else View.GONE
    }

    private fun updateWebUrlEditTextVisibility() {
        webUrlEditText.visibility = if (webUrlCustom.isChecked) View.VISIBLE else View.GONE
    }

    private fun updateWsUrlEditTextVisibility() {
        wsUrlEditText.visibility = if (wsUrlCustom.isChecked) View.VISIBLE else View.GONE
    }

    private fun saveSettings() {
        // 保存Host设置
        val selectedHost = when (hostRadioGroup.checkedRadioButtonId) {
            R.id.host_prod -> DEFAULT_HOST
            R.id.host_test -> TEST_HOST
            R.id.host_custom -> hostEditText.text.toString().trim()
            else -> DEFAULT_HOST
        }

        // 保存WebUrl设置
        val selectedAppFeUrl = when (webUrlRadioGroup.checkedRadioButtonId) {
            R.id.web_url_prod -> DEFAULT_APP_FE_URL
            R.id.web_url_test -> TEST_APP_FE_URL
            R.id.web_url_custom -> webUrlEditText.text.toString().trim()
            else -> DEFAULT_APP_FE_URL
        }

        // 保存WsUrl设置
        val selectedWsUrl = when (wsUrlRadioGroup.checkedRadioButtonId) {
            R.id.ws_url_prod -> DEFAULT_WS_URL
            R.id.ws_url_test -> TEST_WS_URL
            R.id.ws_url_custom -> wsUrlEditText.text.toString().trim()
            else -> DEFAULT_WS_URL
        }

        // 检查自定义输入是否为空
        if ((hostCustom.isChecked && selectedHost.isEmpty()) ||
//            (webUrlCustom.isChecked && selectedWebUrl.isEmpty()) ||
            (wsUrlCustom.isChecked && selectedWsUrl.isEmpty())
        ) {
            Toast.makeText(this, "自定义配置不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        // 保存配置到SP
        WxpConfig.saveBaseUrl(selectedHost)
        WxpConfig.saveWsUrl(selectedWsUrl)
        WxpConfig.saveAppFeUrl(selectedAppFeUrl)

        // 提示用户重启后生效
        Toast.makeText(this, "配置已保存，重启后生效", Toast.LENGTH_SHORT).show()
        // 关闭页面
        finish()
        //退出app
        ThreadUtils.runOnMainThread({ System.exit(0) }, 1000L)
    }
} 