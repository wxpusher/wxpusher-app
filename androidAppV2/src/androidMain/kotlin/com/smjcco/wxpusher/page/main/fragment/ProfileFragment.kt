package com.smjcco.wxpusher.page.main.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smjcco.wxpusher.BuildConfig
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.WxpBaseFragment
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.common.WxpConstants
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.tencent.upgrade.core.UpgradeManager
import com.tencent.upgrade.core.UpgradeReqCallbackForUserManualCheck

/**
 * 个人中心Fragment
 * 对应iOS中的WxpProfileViewController
 */
class ProfileFragment : WxpBaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI(view)
        setupData()
    }

    private fun setupUI(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ProfileAdapter { item ->
            item.action?.invoke()
        }
        recyclerView.adapter = adapter
    }

    private fun setupData() {
        val sectionData = mutableListOf<ProfileSection>()
        val loginInfo = WxpAppDataService.getLoginInfo()
        val uid = loginInfo?.uid ?: ""
        val spt = loginInfo?.spt ?: ""
        val deviceId = loginInfo?.deviceId ?: ""

        if (!BuildConfig.online) {
            sectionData.add(
                ProfileSection(
                    title = "测试菜单",
                    items = listOf(
                        ProfileItem(
                            title = "切换环境",
                            subtitle = WxpConfig.baseUrl,
                            hasArrow = true
                        ) {
                            WxpJumpPageUtils.jumpToTestPanel(requireActivity())
                        }
                    )
                ))
        }

        // 设备和账号
        sectionData.add(
            ProfileSection(
                title = "设备和账号",
                items = listOf(
                    ProfileItem(
                        title = "UID",
                        subtitle = if (uid.isEmpty()) "未登录" else uid,
                        hasArrow = true
                    ) {
                        copyToClipboard(uid, "UID复制成功")
                    },
                    ProfileItem(
                        title = "SPT",
                        subtitle = if (spt.isEmpty()) "未登录" else spt,
                        hasArrow = true
                    ) {
                        copyToClipboard(spt, "UID复制成功")
                    },
                    ProfileItem(
                        title = "设备ID",
                        subtitle = deviceId,
                        hasArrow = true
                    ) {
                        copyToClipboard(deviceId, "设备ID复制成功")
                    },
                    ProfileItem(
                        title = "用户数据",
                        subtitle = "注销手机号",
                        hasArrow = true
                    ) {
                        WxpJumpPageUtils.jumpToUnbind(requireActivity())
                    },
                    ProfileItem(
                        title = "账号信息",
                        subtitle = "管理账号",
                        hasArrow = true
                    ) {
                        WxpJumpPageUtils.jumpToAccountDetail(requireActivity())
                    }
                )
            ))

        // 通知提醒
        sectionData.add(
            ProfileSection(
                title = "通知提醒",
                items = listOf(
                    ProfileItem(
                        title = "通知设置",
                        subtitle = "检查通知权限",
                        hasArrow = true
                    ) {
                        checkNotificationPermission()
                    },
                    ProfileItem(
                        title = "推送检查",
                        subtitle = "收不到消息的异常排查",
                        hasArrow = true
                    ) {
                        openPushCheckUrl()
                    }
                )
            ))

        // 通用
        sectionData.add(
            ProfileSection(
                title = "通用",
                items = listOf(
                    ProfileItem(
                        title = "反馈建议",
                        subtitle = "欢迎你指导我们进步",
                        hasArrow = true
                    ) {
                        openFeedbackUrl()
                    },
                    ProfileItem(
                        title = "软件更新",
                        subtitle = WxpBaseInfoService.getAppVersionName(),
                        hasArrow = true
                    ) {
                        checkForUpdate()
                    },
                    ProfileItem(
                        title = "用户协议",
                        subtitle = "查看用户和隐私协议",
                        hasArrow = true
                    ) {
                        openUserAgreementUrl()
                    },
                    ProfileItem(
                        title = "备案号",
                        subtitle = "蜀ICP备14025423号-2A",
                        hasArrow = true
                    ) {
                        openRecordUrl()
                    }
                )
            ))

        adapter.setData(sectionData)
        adapter.notifyDataSetChanged()
    }

    private fun copyToClipboard(text: String, successMessage: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WxPusher", text)
        clipboard.setPrimaryClip(clip)
        WxpToastUtils.showToast(successMessage)
    }

    private fun checkNotificationPermission() {
        val hasPermission = PermissionUtils.hasNotificationPermission(requireActivity())
        if (hasPermission) {
            WxpToastUtils.showToast("你已经打开通知权限")
            val params = WxpDialogParams(
                title = "提醒方式设置",
                message = "当前已经打开通知权限，你还可以设置锁屏显示、通知中心显示、横幅显示等，还可以设置通知的铃声。是否前往设置？",
                leftText = "取消",
                rightText = "去设置",
                rightBlock = {
                    PermissionUtils.gotoNotificationSettingPage()
                }
            )
            WxpDialogUtils.showDialog(params)
        } else {
            val params = WxpDialogParams(
                title = "异常提醒",
                message = "WxPusher必须要推送权限才能正常工作，请在【设置-WxPusher消息推送平台-通知】打开相关开关",
                leftText = "取消",
                rightText = "去设置",
                rightBlock = {
                    PermissionUtils.gotoNotificationSettingPage()
                }
            )
            WxpDialogUtils.showDialog(params)
        }
    }

    private fun openPushCheckUrl() {
        val url = "https://wxpusher.zjiecode.com/docs/open-app-note/index.html?brand=Android"
        WxpJumpPageUtils.jumpToWebUrl(url, requireActivity())
    }

    private fun openFeedbackUrl() {
        val url = "https://wj.qq.com/s2/22198188/cc95/"
        WxpJumpPageUtils.jumpToWebUrl(url, requireActivity())
    }

    private fun checkForUpdate() {
        try {
            UpgradeManager.getInstance()
                .checkUpgrade(true, null, object : UpgradeReqCallbackForUserManualCheck() {
                    override fun onReceivedNoStrategy() {
                        WxpToastUtils.showToast("已经是最新版本")
                    }
                })
        } catch (e: Exception) {
            WxpToastUtils.showToast("检查更新失败")
        }
    }

    private fun openUserAgreementUrl() {
        WxpJumpPageUtils.jumpToWebUrl(WxpConstants.PrivacyUrl, requireActivity())
    }

    private fun openRecordUrl() {
        val url = "https://beian.miit.gov.cn/"
        WxpJumpPageUtils.jumpToWebUrl(url, requireActivity())
    }

    // 数据模型类
    data class ProfileSection(
        val title: String,
        val items: List<ProfileItem>
    )

    data class ProfileItem(
        val title: String,
        val subtitle: String,
        val hasArrow: Boolean = false,
        val isEnabled: Boolean = true,
        val action: (() -> Unit)? = null
    )

    // RecyclerView适配器
    private class ProfileAdapter(
        private val onItemClick: (ProfileItem) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_ITEM = 1
        }

        private val items = mutableListOf<Any>()


        fun setData(sections: List<ProfileSection>) {
            items.clear()
            sections.forEach { section ->
                items.add(section.title) // 添加section header
                items.addAll(section.items) // 添加section items
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is String -> TYPE_HEADER
                is ProfileItem -> TYPE_ITEM
                else -> TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_HEADER -> {
                    val view = inflater.inflate(R.layout.item_profile_section_header, parent, false)
                    SectionHeaderViewHolder(view)
                }

                else -> {
                    val view = inflater.inflate(R.layout.item_profile, parent, false)
                    ItemViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is SectionHeaderViewHolder -> {
                    val title = items[position] as String
                    holder.bind(title)
                }

                is ItemViewHolder -> {
                    val item = items[position] as ProfileItem
                    // 检查是否是section中的最后一个item
                    val isLastInSection = isLastItemInSection(position)
                    holder.bind(item, onItemClick, isLastInSection)
                }
            }
        }

        private fun isLastItemInSection(position: Int): Boolean {
            // 如果是最后一个item，肯定是section的最后一个
            if (position == items.size - 1) return true

            // 如果下一个item是String类型（section header），说明当前item是section的最后一个
            if (position + 1 < items.size && items[position + 1] is String) return true

            return false
        }

        override fun getItemCount(): Int = items.size

        // Section Header ViewHolder
        private class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView = itemView.findViewById(R.id.tv_section_title)

            fun bind(title: String) {
                titleTextView.text = title
            }
        }

        // Item ViewHolder
        private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView = itemView.findViewById(R.id.tv_title)
            private val subtitleTextView: TextView = itemView.findViewById(R.id.tv_subtitle)
            private val arrowImageView: ImageView = itemView.findViewById(R.id.iv_arrow)
            private val dividerView: View = itemView.findViewById(R.id.divider)

            fun bind(
                item: ProfileItem,
                onItemClick: (ProfileItem) -> Unit,
                isLastInSection: Boolean = false
            ) {
                titleTextView.text = item.title
                subtitleTextView.text = item.subtitle
                arrowImageView.visibility = if (item.hasArrow) View.VISIBLE else View.GONE

                // 控制分割线显示：如果是section中的最后一个item，隐藏分割线
                dividerView.visibility = if (isLastInSection) View.GONE else View.VISIBLE

                itemView.isEnabled = item.isEnabled
                itemView.alpha = if (item.isEnabled) 1.0f else 0.5f

                if (item.isEnabled) {
                    itemView.setOnClickListener {
                        onItemClick(item)
                    }
                } else {
                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                }
            }
        }
    }
}
