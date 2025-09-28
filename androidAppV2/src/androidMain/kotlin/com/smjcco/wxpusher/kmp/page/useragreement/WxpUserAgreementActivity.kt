package com.smjcco.wxpusher.kmp.page.useragreement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.kmp.base.WxpBaseActivity
import com.smjcco.wxpusher.kmp.common.WxpSaveKey
import com.smjcco.wxpusher.kmp.common.utils.WxpJumpPageUtils
import com.smjcco.wxpusher.utils.PermissionUtils

class WxpUserAgreementActivity : WxpBaseActivity() {

    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var agreeButton: MaterialButton
    private lateinit var disagreeButton: MaterialButton

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WxpUserAgreementActivity::class.java)
            context.startActivity(intent)
        }

        private const val USER_AGREEMENT_URL =
            "https://wxpusher.zjiecode.com/admin/agreement/index-argeement.html"
        private const val PRIVACY_POLICY_URL =
            "https://wxpusher.zjiecode.com/admin/agreement/privacy-agreement.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement)
        supportActionBar?.hide()

        setupPermissionLauncher()
        initViews()
        setupClickableText()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        // 检查通知权限状态，如果已经打开，可能需要重新注册推送
        checkNotificationPermissionOnResume()
    }

    private fun setupPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            handlePermissionResult(isGranted)
        }
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
                    WxpJumpPageUtils.jumpToWebUrl(USER_AGREEMENT_URL, this@WxpUserAgreementActivity)
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
                    WxpJumpPageUtils.jumpToWebUrl(PRIVACY_POLICY_URL, this@WxpUserAgreementActivity)
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

    private fun onAgreeClicked() {
        // 请求通知权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (PermissionUtils.hasNotificationPermission(this)) {
                // 已经有权限，直接跳转主页面
                jumpToMain()
            } else {
                // 请求权限
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 13以下，检查通知是否启用
            if (PermissionUtils.hasNotificationPermission(this)) {
                jumpToMain()
            } else {
                showNotificationSettingsDialog()
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

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            jumpToMain()
        } else {
            showNotificationSettingsDialog()
        }
    }

    private fun showNotificationSettingsDialog() {
        val params = WxpDialogParams(
            title = "异常提醒",
            message = "WxPusher必须要推送权限才能正常工作，你可以稍后在【设置-WxPusher消息推送平台-通知】中打开",
            leftText = "取消",
            leftBlock = {
                jumpToMain()
            },
            rightText = "去设置",
            rightBlock = {
                WxpJumpPageUtils.openAppSettings(this@WxpUserAgreementActivity)
            }
        )
        WxpDialogUtils.showDialog(params)
    }

    private fun jumpToMain() {
        // 保存用户已同意协议
        WxpSaveService.set(WxpSaveKey.UserHasAgreement, true)
        // 跳转到主页面
        WxpJumpPageUtils.jumpToMain(this)
        finish()
    }

    private fun checkNotificationPermissionOnResume() {
        // 当从设置页面返回时，检查权限状态，如果已开启则注册推送
        if (PermissionUtils.hasNotificationPermission(this)) {
            // TODO: 这里可能需要触发推送注册，但现在先留空，因为主要在jumpToMain中处理
        }
    }
}