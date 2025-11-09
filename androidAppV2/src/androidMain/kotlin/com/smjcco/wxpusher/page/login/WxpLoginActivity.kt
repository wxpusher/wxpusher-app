package com.smjcco.wxpusher.page.login

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.smjcco.wxpusher.BuildConfig
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.common.WxpConstants
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.smjcco.wxpusher.wxapi.WxpWeixinOpenManager

class WxpLoginActivity : WxpBaseMvpActivity<IWxpLoginPresenter>(), IWxpLoginView {

    private lateinit var phoneTextField: TextInputEditText
    private lateinit var codeTextField: TextInputEditText
    private lateinit var getCodeButton: MaterialButton
    private lateinit var loginButton: MaterialButton
    private lateinit var privacyCheckbox: MaterialCheckBox
    private lateinit var privacyLabel: TextView
    private lateinit var weixinLoginContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
        setupPrivacyLabel()

        presenter.init()

        //测试包，添加进入测试面板入口
        if (!BuildConfig.online) {
            val titleView = findViewById<View>(R.id.title_label)
            titleView.setOnClickListener {
                WxpJumpPageUtils.jumpToTestPanel(this)
            }
        }
    }

    private fun initViews() {
        supportActionBar?.hide()
        phoneTextField = findViewById(R.id.phone_text_field)
        codeTextField = findViewById(R.id.code_text_field)
        getCodeButton = findViewById(R.id.get_code_button)
        loginButton = findViewById(R.id.login_button)
        privacyCheckbox = findViewById(R.id.privacy_checkbox)
        privacyLabel = findViewById(R.id.privacy_label)
        weixinLoginContainer = findViewById(R.id.weixin_login_container)
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

        weixinLoginContainer.setOnClickListener {
            onWeixinLoginClick()
        }

    }

    private fun onWeixinLoginClick() {
        WxpWeixinOpenManager.requestAuth { response, error ->
            if (error != null) {
                WxpToastUtils.showToast(error.message)
                return@requestAuth
            }
            presenter.wexinLogin(response?.code)
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
        WxpJumpPageUtils.jumpToWebUrl(WxpConstants.PrivacyUrl, this)
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
        WxpJumpPageUtils.jumpToBind(phone, code, data.phoneVerifyCode ?: "", this)
        finish()
    }

    override fun onGoMain() {
        WxpJumpPageUtils.jumpToMain(this)
        finish()
    }
}