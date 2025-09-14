package com.smjcco.wxpusher.kmp.login

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.kmp.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.kmp.bind.WxpBindActivity
import com.smjcco.wxpusher.kmp.main.WxpMainActivity
import com.smjcco.wxpusher.page.login.IWxpLoginPresenter
import com.smjcco.wxpusher.page.login.IWxpLoginView
import com.smjcco.wxpusher.page.login.WxpLoginPresenter
import com.smjcco.wxpusher.page.login.WxpLoginSendVerifyCodeResp

class WxpLoginActivity : WxpBaseMvpActivity<IWxpLoginPresenter>(), IWxpLoginView {

    private lateinit var phoneTextField: com.google.android.material.textfield.TextInputEditText
    private lateinit var codeTextField: com.google.android.material.textfield.TextInputEditText
    private lateinit var getCodeButton: com.google.android.material.button.MaterialButton
    private lateinit var loginButton: com.google.android.material.button.MaterialButton
    private lateinit var privacyCheckbox: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var privacyLabel: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
        setupPrivacyLabel()

        presenter.init()
    }

    private fun initViews() {
        phoneTextField = findViewById(R.id.phone_text_field)
        codeTextField = findViewById(R.id.code_text_field)
        getCodeButton = findViewById(R.id.get_code_button)
        loginButton = findViewById(R.id.login_button)
        privacyCheckbox = findViewById(R.id.privacy_checkbox)
        privacyLabel = findViewById(R.id.privacy_label)
    }

    private fun setupClickListeners() {
        getCodeButton.setOnClickListener {
            val phone = phoneTextField.text?.toString()
            presenter.sendVerifyCode(phone = phone)
        }

        loginButton.setOnClickListener {
            if (!privacyCheckbox.isChecked) {
                WxpToastUtils.showToast(getString(R.string.login_agree_privacy_first))
                return@setOnClickListener
            }

            val phone = phoneTextField.text?.toString()
            val code = codeTextField.text?.toString()
            presenter.verifyCodeLogin(phone = phone, verifyCode = code)
        }

    }

    private fun setupPrivacyLabel() {
        val text = getString(R.string.login_privacy_text)
        val spannableString = SpannableString(text)

        // 设置"隐私协议和用户协议"部分为可点击的蓝色文字
        val titleText = "隐私协议和用户协议"
        val startIndex = text.indexOf(titleText)
        val endIndex = startIndex + titleText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // 跳转到隐私协议页面
                jumpToPrivacy()
            }
        }

        spannableString.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        privacyLabel.text = spannableString
        privacyLabel.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun jumpToPrivacy() {
        // TODO: 实现跳转到隐私协议页面
        WxpBindActivity.start(this, "13002899981", "110120", "BIND_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
    }

    override fun createPresenter(): IWxpLoginPresenter {
        return WxpLoginPresenter(this)
    }

    override fun onSendButtonText(msg: String, loading: Boolean) {
        runOnUiThread {
            if (loading) {
                getCodeButton.isEnabled = false
                getCodeButton.text = getString(R.string.login_sending)
            } else {
                getCodeButton.isEnabled = true
                getCodeButton.text = msg
            }
        }
    }

    override fun onGoBind(phone: String, code: String, data: WxpLoginSendVerifyCodeResp) {
        // TODO: 实现跳转到绑定手机页面
//        startActivity(Intent(this, WxpMainActivity::class.java))
//        finish()
    }

    override fun onGoMain() {
        startActivity(Intent(this, WxpMainActivity::class.java))
        finish()
    }
}