package com.smjcco.wxpusher.page.registerorbind

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpLoadingUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.page.login.WxpBindPageData
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.smjcco.wxpusher.wxapi.WeChatAuthResponse
import com.smjcco.wxpusher.wxapi.WeChatError
import com.smjcco.wxpusher.wxapi.WxpWeixinOpenManager

class WxpRegisterOrBindActivity : WxpBaseMvpActivity<IWxpRegisterOrBindPresenter>(),
    IWxpRegisterOrBindView {

    companion object {
        private const val EXTRA_BIND_PAGE_DATA = "extra_bind_page_data"

        fun start(context: android.content.Context, bindPageData: WxpBindPageData) {
            val intent = Intent(context, WxpRegisterOrBindActivity::class.java)
            intent.putExtra(EXTRA_BIND_PAGE_DATA, bindPageData.toJson())
            context.startActivity(intent)
        }
    }

    private lateinit var descriptionLabel: TextView
    private lateinit var optionOneContainer: View
    private lateinit var optionOneRecommendLabel: TextView
    private lateinit var optionOneIcon: ImageView
    private lateinit var optionOneTitleLabel: TextView
    private lateinit var optionOneDescLabel: TextView
    private lateinit var optionOneButton: MaterialButton

    private lateinit var optionTwoContainer: View
    private lateinit var optionTwoIcon: ImageView
    private lateinit var optionTwoTitleLabel: TextView
    private lateinit var optionTwoDescLabel: TextView
    private lateinit var optionTwoButton: MaterialButton

    private lateinit var optionThreeContainer: View
    private lateinit var optionThreeWarningLabel: TextView
    private lateinit var optionThreeIcon: ImageView
    private lateinit var optionThreeTitleLabel: TextView
    private lateinit var optionThreeDescLabel: TextView
    private lateinit var optionThreeButton: MaterialButton

    private lateinit var bindPageData: WxpBindPageData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_or_bind)

        // 获取传递的参数
        val json = intent.getStringExtra(EXTRA_BIND_PAGE_DATA)
        bindPageData = WxpBindPageData.fromJson(json) ?: return

        // 设置标题
        title = getString(R.string.register_or_bind_title)

        // 初始化视图
        initViews()

        // 设置点击事件
        setupClickListeners()

        // 更新UI状态
        updateUIState()
    }

    private fun initViews() {
        descriptionLabel = findViewById(R.id.description_label)
        optionOneContainer = findViewById(R.id.option_one_container)
        optionOneRecommendLabel = findViewById(R.id.option_one_recommend_label)
        optionOneIcon = findViewById(R.id.option_one_icon)
        optionOneTitleLabel = findViewById(R.id.option_one_title_label)
        optionOneDescLabel = findViewById(R.id.option_one_desc_label)
        optionOneButton = findViewById(R.id.option_one_button)

        optionTwoContainer = findViewById(R.id.option_two_container)
        optionTwoIcon = findViewById(R.id.option_two_icon)
        optionTwoTitleLabel = findViewById(R.id.option_two_title_label)
        optionTwoDescLabel = findViewById(R.id.option_two_desc_label)
        optionTwoButton = findViewById(R.id.option_two_button)

        optionThreeContainer = findViewById(R.id.option_three_container)
        optionThreeWarningLabel = findViewById(R.id.option_three_warning_label)
        optionThreeIcon = findViewById(R.id.option_three_icon)
        optionThreeTitleLabel = findViewById(R.id.option_three_title_label)
        optionThreeDescLabel = findViewById(R.id.option_three_desc_label)
        optionThreeButton = findViewById(R.id.option_three_button)
    }

    private fun setupClickListeners() {
        optionOneButton.setOnClickListener {
            onOptionOneButtonClick()
        }

        optionTwoButton.setOnClickListener {
            onOptionTwoButtonClick()
        }

        optionThreeButton.setOnClickListener {
            onOptionThreeButtonClick()
        }
    }

    private fun updateUIState() {
        // 如果是手机号登录，显示选项2；Android不需要苹果登录
        val isPhoneLogin = bindPageData.phoneLogin != null
        optionTwoContainer.visibility = if (isPhoneLogin) View.VISIBLE else View.GONE
    }

    private fun onOptionOneButtonClick() {
        // 微信一键绑定
        WxpLoadingUtils.showLoading("微信授权中", true)
        WxpWeixinOpenManager.requestAuth { response: WeChatAuthResponse?, error: WeChatError? ->
            WxpLoadingUtils.dismissLoading()
            if (error != null) {
                WxpToastUtils.showToast(error.message)
                return@requestAuth
            }
            response?.let {
                presenter.weixinBind(it.code, bindPageData)
            }
        }
    }

    private fun onOptionTwoButtonClick() {
        // 通过微信公众号绑定
        val phoneLogin = bindPageData.phoneLogin
        phoneLogin?.let {
            WxpJumpPageUtils.jumpToMpBind(it, this)
        }
    }

    private fun onOptionThreeButtonClick() {
        // 创建新账号 - 需要确认对话框
        val params = WxpDialogParams()
        params.title = "确认创建新账号？"
        params.message =
            "创建新账号后，会和已有微信账号完全分开，不能和微信账号的数据一致，也不能收到微信的消息。\n\n强烈建议绑定微信创建账号（即使你之前没有用过微信账号也可以绑定）。"
        params.leftText = "取消"
        params.rightText = "确认创建"
        params.rightBlock = {
            handleCreateNewAccount()
        }
        WxpDialogUtils.showDialog(params)
    }

    // 手机号或者苹果登录的时候，创建新账号
    private fun handleCreateNewAccount() {
        presenter.createAccount(bindPageData)
    }

    override fun createPresenter(): IWxpRegisterOrBindPresenter {
        return WxpRegisterOrBindPresenter(this)
    }

    override fun onGoMain() {
        WxpJumpPageUtils.jumpToMain(this)
        finish()
    }
}

