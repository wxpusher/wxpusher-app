package com.smjcco.wxpusher.kmp.page.unbind

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.kmp.base.WxpBaseActivity

class WxpUnbindActivity : WxpBaseActivity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WxpUnbindActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var warningIcon: ImageView
    private lateinit var titleLabel: TextView
    private lateinit var descriptionLabel: TextView
    private lateinit var confirmationCard: CardView
    private lateinit var confirmationText: TextView
    private lateinit var inputLayout: TextInputLayout
    private lateinit var inputTextField: TextInputEditText
    private lateinit var confirmButton: MaterialButton

    private val requiredText = "注销账号"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unbind)

        // 设置标题和导航
        title = getString(R.string.unbind_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化视图
        initViews()

        // 设置监听器
        setupListeners()
    }

    private fun initViews() {
        warningIcon = findViewById(R.id.warning_icon)
        titleLabel = findViewById(R.id.title_label)
        descriptionLabel = findViewById(R.id.description_label)
        confirmationCard = findViewById(R.id.confirmation_card)
        confirmationText = findViewById(R.id.confirmation_text)
        inputLayout = findViewById(R.id.input_layout)
        inputTextField = findViewById(R.id.input_text_field)
        confirmButton = findViewById(R.id.confirm_button)

    }

    private fun setupListeners() {
        // 输入框文本变化监听
        inputTextField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s?.toString()?.trim() ?: ""
                val enable = requiredText == inputText
                confirmButton.backgroundTintList = ContextCompat.getColorStateList(
                    this@WxpUnbindActivity,
                    if (enable) R.color.danger_color else android.R.color.darker_gray
                )
            }
        })

        // 确认按钮点击监听
        confirmButton.setOnClickListener {
            if (inputTextField.text.toString() == requiredText) {
                WxpAppDataService.unbindPhone()
            } else {
                WxpToastUtils.showToast("请输入【$requiredText】")
            }
        }
    }

    //点击导航栏的返回
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}
