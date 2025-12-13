package com.smjcco.wxpusher.page.accountdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpMaskUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.smjcco.wxpusher.wxapi.WxpWeixinOpenManager

class AccountDetailActivity : WxpBaseMvpActivity<WxpAccountDetailPresenter>(),
    IWxpAccountDetailView {

    private lateinit var recyclerView: RecyclerView
    private val menuItems = mutableListOf<AccountMenuItem>()
    private lateinit var adapter: AccountDetailAdapter

    override fun createPresenter(): WxpAccountDetailPresenter {
        return WxpAccountDetailPresenter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "账号信息"

        initViews()
        setupData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from other pages (e.g. change phone)
        setupData()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AccountDetailAdapter()
        recyclerView.adapter = adapter
    }

    private fun setupData() {
        menuItems.clear()
        val loginInfo = WxpAppDataService.getLoginInfo()
        val maskPhone = if (!loginInfo?.phone.isNullOrEmpty()) {
            WxpMaskUtils.mask(loginInfo?.phone!!, 3, 4)
        } else {
            "未绑定"
        }

        // Phone Item
        menuItems.add(
            AccountMenuItem(
                iconRes = R.drawable.ic_phone,
                title = "手机账号",
                value = maskPhone,
                showArrow = true,
                action = { WxpJumpPageUtils.jumpToChangePhone(this) }
            ))

        // WeChat Item
        val weixinBind = loginInfo?.weiXinBind == true
        menuItems.add(
            AccountMenuItem(
                iconRes = R.drawable.ic_weixin, // Ensure this resource exists or use placeholder
                title = "微信绑定",
                value = if (weixinBind) "已绑定" else "未绑定",
                showArrow = !weixinBind,
                action = if (weixinBind) null else { -> handleBindWeixinTap() }
            ))

        // Apple Item - Hidden for Android as typically not supported directly like iOS unless configured
        // If we want to show it as status:
        if (loginInfo?.appleBind == true) {
            menuItems.add(
                AccountMenuItem(
                    iconRes = R.drawable.ic_cloud, // Placeholder for Apple
                    title = "Apple账号",
                    value = "已绑定",
                    showArrow = false,
                    action = null
                )
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun handleBindWeixinTap() {
        WxpWeixinOpenManager.requestAuth { response, error ->
            if (error != null) {
                WxpToastUtils.showToast("微信授权失败: ${error.message}")
                return@requestAuth
            }
            if (response != null) {
                presenter.weixinBind(response.code)
            }
        }
    }

    override fun onWeixinBindSuccess() {
        WxpToastUtils.showToast("微信绑定成功")
        setupData()
    }

    override fun onAppleBindSuccess() {
        // Not used on Android typically
        setupData()
    }

    private fun handleLogoutTap() {
        presenter.logout()
    }

    private fun handleDeleteAccountTap() {
        AlertDialog.Builder(this)
            .setTitle("删除账号")
            .setMessage("确定要删除当前账号吗？删除后所有数据将无法恢复。")
            .setPositiveButton("确定") { _, _ ->
                WxpAppDataService.removeAccount()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    data class AccountMenuItem(
        val iconRes: Int,
        val title: String,
        val value: String,
        val showArrow: Boolean,
        val action: (() -> Unit)?
    )

    inner class AccountDetailAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_ITEM = 0
        private val TYPE_FOOTER = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_ITEM) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_account_menu, parent, false)
                ItemViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_account_footer, parent, false)
                FooterViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == TYPE_ITEM) {
                (holder as ItemViewHolder).bind(menuItems[position], position == menuItems.size - 1)
            } else {
                (holder as FooterViewHolder).bind()
            }
        }

        override fun getItemCount(): Int {
            return menuItems.size + 1 // Items + Footer
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < menuItems.size) TYPE_ITEM else TYPE_FOOTER
        }

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
            private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            private val tvValue: TextView = itemView.findViewById(R.id.tv_value)
            private val ivArrow: ImageView = itemView.findViewById(R.id.iv_arrow)
            private val divider: View = itemView.findViewById(R.id.divider)

            fun bind(item: AccountMenuItem, isLast: Boolean) {
                try {
                    ivIcon.setImageResource(item.iconRes)
                } catch (e: Exception) {
                    // Fallback or ignore if resource missing
                }

                tvTitle.text = item.title
                tvValue.text = item.value

                ivArrow.visibility = if (item.showArrow) View.VISIBLE else View.INVISIBLE
                divider.visibility = if (isLast) View.GONE else View.VISIBLE

                itemView.setOnClickListener {
                    item.action?.invoke()
                }

                // Disable click effect if no action
                itemView.isClickable = item.action != null
            }
        }

        inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val btnLogout: Button = itemView.findViewById(R.id.btn_logout)
            private val btnDelete: TextView = itemView.findViewById(R.id.btn_delete_account)

            fun bind() {
                btnLogout.setOnClickListener { handleLogoutTap() }
                btnDelete.setOnClickListener { handleDeleteAccountTap() }
            }
        }
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

