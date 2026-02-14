package com.smjcco.wxpusher.page.changephone

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpMaskUtils

class WxpChangePhoneActivity : WxpBaseMvpActivity<IWxpChangePhonePresenter>(), IWxpChangePhoneView {

    private lateinit var tvCurrentPhone: TextView
    private lateinit var etNewPhone: EditText
    private lateinit var etVerifyCode: EditText
    private lateinit var tvSendCode: TextView
    private lateinit var btnConfirm: Button

    override fun createPresenter(): IWxpChangePhonePresenter {
        return WxpChangePhonePresenter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wxp_change_phone)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "换绑手机"
        
        initView()
        initData()
        initListener()
    }

    private fun initView() {
        tvCurrentPhone = findViewById(R.id.tvCurrentPhone)
        etNewPhone = findViewById(R.id.etNewPhone)
        etVerifyCode = findViewById(R.id.etVerifyCode)
        tvSendCode = findViewById(R.id.tvSendCode)
        btnConfirm = findViewById(R.id.btnConfirm)
    }

    private fun initData() {
        val loginInfo = WxpAppDataService.getLoginInfo()
        val phone = loginInfo?.phone
        if (!phone.isNullOrEmpty()) {
            tvCurrentPhone.text = "当前绑定手机号: ${WxpMaskUtils.mask(phone, 3, 4)}"
            tvCurrentPhone.visibility = View.VISIBLE
        } else {
            tvCurrentPhone.visibility = View.GONE
        }
    }

    private fun initListener() {
        tvSendCode.setOnClickListener {
            val phone = etNewPhone.text.toString().trim()
            presenter.sendVerifyCode(phone)
        }

        btnConfirm.setOnClickListener {
            val phone = etNewPhone.text.toString().trim()
            val code = etVerifyCode.text.toString().trim()
            presenter.bindPhone(phone, code)
        }
    }

    override fun onSendButtonText(msg: String) {
        tvSendCode.text = msg
    }

    override fun onChangPhoneFinish() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

