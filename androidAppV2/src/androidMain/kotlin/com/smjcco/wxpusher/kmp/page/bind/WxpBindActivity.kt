package com.smjcco.wxpusher.kmp.page.bind

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.button.MaterialButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.kmp.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.kmp.page.main.WxpMainActivity
import com.smjcco.wxpusher.page.bind.IWxpBindView
import com.smjcco.wxpusher.page.bind.WxpBindPresenter

class WxpBindActivity : WxpBaseMvpActivity<WxpBindPresenter>(), IWxpBindView {
    
    companion object {
        private const val EXTRA_PHONE = "extra_phone"
        private const val EXTRA_CODE = "extra_code"
        private const val EXTRA_PHONE_VERIFY_CODE = "extra_phone_verify_code"
        
        fun start(context: Context, phone: String, code: String, phoneVerifyCode: String) {
            val intent = Intent(context, WxpBindActivity::class.java).apply {
                putExtra(EXTRA_PHONE, phone)
                putExtra(EXTRA_CODE, code)
                putExtra(EXTRA_PHONE_VERIFY_CODE, phoneVerifyCode)
            }
            context.startActivity(intent)
        }
    }
    
    private lateinit var codeTextField: AppCompatTextView
    private lateinit var copyButton: MaterialButton
    private lateinit var checkStatusButton: MaterialButton
    private lateinit var loadingIndicator: ProgressBar
    
    private var phone: String = ""
    private var code: String = ""
    private var phoneVerifyCode: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bind)
        
        // 获取传递的参数
        phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        code = intent.getStringExtra(EXTRA_CODE) ?: ""
        phoneVerifyCode = intent.getStringExtra(EXTRA_PHONE_VERIFY_CODE) ?: ""
        
        // 设置标题
        title = getString(R.string.bind_page_title)
        
        // 初始化视图
        initViews()
        
        // 设置点击事件
        setupClickListeners()
        
        // 设置绑定码
        codeTextField.setText(phoneVerifyCode)
    }
    
    private fun initViews() {
        codeTextField = findViewById(R.id.code_text_field)
        copyButton = findViewById(R.id.copy_button)
        checkStatusButton = findViewById(R.id.check_status_button)
        loadingIndicator = findViewById(R.id.loading_indicator)
    }
    
    private fun setupClickListeners() {
        copyButton.setOnClickListener {
            copyCodeToClipboard()
        }
        
        checkStatusButton.setOnClickListener {
            queryBindStatus()
        }
    }
    
    private fun copyCodeToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("绑定码", phoneVerifyCode)
        clipboard.setPrimaryClip(clip)
        WxpToastUtils.showToast(getString(R.string.bind_copy_success))
    }
    
    private fun queryBindStatus() {
        presenter.queryBindStatus(phone, code)
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            loadingIndicator.visibility = View.VISIBLE
            checkStatusButton.isEnabled = false
            checkStatusButton.text = getString(R.string.login_sending)
        } else {
            loadingIndicator.visibility = View.GONE
            checkStatusButton.isEnabled = true
            checkStatusButton.text = getString(R.string.bind_check_status_button)
        }
    }
    
    override fun createPresenter(): WxpBindPresenter {
        return WxpBindPresenter(this)
    }
    
    override fun onGoMain() {
        // 跳转到主页面
        val intent = Intent(this, WxpMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}