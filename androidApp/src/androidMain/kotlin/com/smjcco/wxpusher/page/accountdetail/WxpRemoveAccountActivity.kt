package com.smjcco.wxpusher.page.accountdetail

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.base.biz.WxpAppDataService

class WxpRemoveAccountActivity : WxpBaseActivity() {

    private lateinit var etInput: EditText
    private lateinit var btnConfirm: Button
    private val requiredText = "删除账号"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wxp_remove_account)
        
        // 设置标题
        title = "删除账号"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initView()
        initListener()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initView() {
        etInput = findViewById(R.id.etInput)
        btnConfirm = findViewById(R.id.btnConfirm)
    }

    private fun initListener() {
        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                checkInputState(s.toString().trim())
            }
        })

        btnConfirm.setOnClickListener {
            showConfirmDialog()
        }
    }

    private fun checkInputState(inputText: String) {
        val isValid = inputText == requiredText
        btnConfirm.isEnabled = isValid
        
        // 更新输入框边框颜色
        val background = etInput.background as? GradientDrawable
        if (background != null) {
            val strokeColor = when {
                inputText.isEmpty() -> ContextCompat.getColor(this, R.color.input_border_color)
                isValid -> ContextCompat.getColor(this, R.color.input_border_valid)
                else -> ContextCompat.getColor(this, R.color.input_border_invalid)
            }
            background.setStroke(dp2px(1f), strokeColor)
        }
    }

    private fun showConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("最后一次提示")
            .setMessage("删除后账号将无法使用，并且删除所有数据，无法进行找回，点击确认后，立即删除账号，是否继续？")
            .setPositiveButton("确认删除") { _, _ ->
                WxpAppDataService.removeAccount()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dp2px(dp: Float): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}

