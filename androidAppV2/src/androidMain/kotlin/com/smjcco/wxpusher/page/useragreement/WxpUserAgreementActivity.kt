package com.smjcco.wxpusher.page.useragreement

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.common.WxpConstants
import com.smjcco.wxpusher.common.WxpSaveKey
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

class WxpUserAgreementActivity : WxpBaseActivity() {

    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var agreeButton: MaterialButton
    private lateinit var disagreeButton: MaterialButton

    private var permissionRequester: PermissionRequester? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WxpUserAgreementActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement)
        supportActionBar?.hide()

        initViews()
        setupClickableText()
        setupButtons()
        setUpPermissionRequester()
    }


    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        descriptionText = findViewById(R.id.descriptionText)
        agreeButton = findViewById(R.id.agreeButton)
        disagreeButton = findViewById(R.id.disagreeButton)
    }

    private fun setupClickableText() {
        val text = descriptionText.text.toString()
        val spannableString = SpannableStringBuilder(text)

        // 查找"用户协议"
        val userAgreementText = "用户协议"
        val userAgreementStart = text.indexOf(userAgreementText)
        if (userAgreementStart != -1) {
            val userAgreementEnd = userAgreementStart + userAgreementText.length
            val userAgreementSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    WxpJumpPageUtils.jumpToWebUrl(
                        WxpConstants.PrivacyUrl,
                        this@WxpUserAgreementActivity
                    )
                }
            }
            spannableString.setSpan(
                userAgreementSpan,
                userAgreementStart,
                userAgreementEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimary
                    )
                ), userAgreementStart, userAgreementEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                UnderlineSpan(),
                userAgreementStart,
                userAgreementEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 查找"隐私政策"
        val privacyPolicyText = "隐私政策"
        val privacyPolicyStart = text.indexOf(privacyPolicyText)
        if (privacyPolicyStart != -1) {
            val privacyPolicyEnd = privacyPolicyStart + privacyPolicyText.length
            val privacyPolicySpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    WxpJumpPageUtils.jumpToWebUrl(
                        WxpConstants.PrivacyPolicyUrl,
                        this@WxpUserAgreementActivity
                    )
                }
            }
            spannableString.setSpan(
                privacyPolicySpan,
                privacyPolicyStart,
                privacyPolicyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this,
                        R.color.colorPrimary
                    )
                ), privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                UnderlineSpan(),
                privacyPolicyStart,
                privacyPolicyEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        descriptionText.text = spannableString
        descriptionText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupButtons() {
        agreeButton.setOnClickListener {
            onAgreeClicked()
        }

        disagreeButton.setOnClickListener {
            onDisagreeClicked()
        }
    }

    private fun setUpPermissionRequester() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
            return
        }

        permissionRequester = PermissionRequester(
            this,  Manifest.permission.POST_NOTIFICATIONS,
            "需要发送通知权限",
            "WxPusher是一个消息推送平台，当有新消息到达的时候，我们会第一时间给你发送通知，因此需要你授予发送通知的权限，否则我们无法发送消息通知，你可能会因此遗漏消息，是否授予权限？",
            "缺少必须的通知权限",
            "本应用核心功能是发送消息通知，缺少通知权限会导致你无法收到消息通知。\n\n打开方式：点击“去设置”-“通知管理”-打开允许通知"
        ) {
            PermissionUtils.gotoNotificationSettingPage()
        }
    }

    private fun onAgreeClicked() {
        //用户点击后，直接保存用户同意协议
        WxpSaveService.set(WxpSaveKey.UserHasAgreement, true)

        // 已经有权限，直接跳转主页面
        if (PermissionUtils.hasNotificationPermission(this)) {
            jumpToMain()
            return
        }

        //没有通知权限， 就请求通知权限
        requestPermission()
    }


    /**
     * 没有权限的时候，请求权限
     */
    private fun requestPermission() {
        // 请求通知权限
        permissionRequester?.request {
            if (it) {
                jumpToMain()
            } else {
                WxpLogUtils.i(message = "用户拒绝了通知权限")
                WxpToastUtils.showToast("你拒绝了通知权限，将无法给你发送通知提醒")
                jumpToMain()
            }
        }
    }

    private fun onDisagreeClicked() {
        val params = WxpDialogParams(
            title = "",
            message = "WxPusher需要你同意用户和隐私协议，我们才能获取到相关设备信息，才能实现消息推送的功能，如您不同意，软件无法正常工作。您是否同意相关协议？",
            leftText = "取消",
            rightText = "同意",
            rightBlock = {
                onAgreeClicked()
            }
        )
        WxpDialogUtils.showDialog(params)
    }

    private fun jumpToMain() {
        // 跳转到主页面
        WxpJumpPageUtils.jumpToMain(this)
        finish()
    }
}