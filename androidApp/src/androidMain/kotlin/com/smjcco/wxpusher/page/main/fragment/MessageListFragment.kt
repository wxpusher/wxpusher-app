package com.smjcco.wxpusher.page.main.fragment

import android.R.*
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.WxpBaseMvpFragment
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.dialog.ActionSheetDialogFragment
import com.smjcco.wxpusher.dialog.ActionSheetItem
import com.smjcco.wxpusher.page.main.WxpMainActivity
import com.smjcco.wxpusher.page.messagelist.IWxpMessageListPresenter
import com.smjcco.wxpusher.page.messagelist.IWxpMessageListView
import com.smjcco.wxpusher.page.messagelist.WxpCheckAppMsgReasonResp
import com.smjcco.wxpusher.page.messagelist.WxpListBannerResp
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import com.smjcco.wxpusher.page.messagelist.WxpMessageListPresenter
import com.smjcco.wxpusher.page.messagelist.WxpMessageListReq
import com.smjcco.wxpusher.page.web.WxpWebViewActivity
import com.smjcco.wxpusher.push.IPushTokenChangedListener
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils

/**
 * 消息列表Fragment
 * 对应iOS中的MessageListViewController
 */
class MessageListFragment : WxpBaseMvpFragment<IWxpMessageListPresenter>(), IWxpMessageListView,
    ITabMenuProvider, IPushTokenChangedListener {

    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: MessageListAdapter
    private lateinit var searchEditText: EditText
    private lateinit var searchCancelBtn: TextView
    private lateinit var searchBarContainer: LinearLayout

    private var messageList = mutableListOf<WxpMessageListMessage>()
    private var openAppFirstRefresh = true


    private lateinit var refreshHeader: ClassicsHeader

    private lateinit var batteryBanner: View
    private lateinit var bannerBtn: MaterialButton
    private lateinit var bannerCloseImg: AppCompatImageView

    private lateinit var notePermissionBanner: View
    private lateinit var notePermissionTv: TextView
    private lateinit var notePermissionBtn: MaterialButton
    private lateinit var notePermissionCloseImg: AppCompatImageView


    private lateinit var listNoteBanner: View
    private lateinit var listNoteBannerTv: TextView
    private lateinit var listNoteBannerMore: View

    // 批量操作相关
    private lateinit var batchActionBar: View
    private lateinit var batchActionMarkRead: View
    private lateinit var batchActionMarkUnread: View
    private lateinit var batchActionDelete: View

    // 多选顶部栏
    private lateinit var batchTopBar: View
    private lateinit var batchTopBarClose: View
    private lateinit var batchTopBarTitle: TextView
    private lateinit var batchTopBarSelectAll: TextView

    private var selectionMode: Boolean = false
    private val selectedIds: MutableSet<Long> = mutableSetOf()

    companion object {
        private const val MAX_BATCH_SELECT = 200
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_message_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRefreshLayout()
        setupRecyclerView()
        presenter.init()
        //打开就刷新一次
        refreshLayout.autoRefresh()
        refreshBanner()
        refreshNotePermissionBanner()
        //监听pushToken变化，刷新banner
        PushManager.addPushTokenChangedListener(this)
    }

    override fun onPushToken(platform: DevicePlatform, pushToken: String) {
        if (platform == DevicePlatform.Android) {
            refreshBanner()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBanner()
        refreshNotePermissionBanner()
        presenter.fetchListBanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        PushManager.removePushTokenChangedListener(this)
    }

    private fun initViews(view: View) {
        refreshLayout = view.findViewById(R.id.refresh_layout)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyText = view.findViewById(R.id.empty_text)
        searchEditText = view.findViewById(R.id.search_edit_text)
        searchCancelBtn = view.findViewById(R.id.search_cancel_btn)
        searchBarContainer = view.findViewById(R.id.search_bar_container)
        //banner
        batteryBanner = view.findViewById(R.id.battery_banner)
        bannerBtn = view.findViewById(R.id.main_banner_btn)
        bannerCloseImg = view.findViewById(R.id.main_banner_close)

        notePermissionBanner = view.findViewById(R.id.note_permission_banner)
        notePermissionTv = view.findViewById(R.id.note_permission_banner_text)
        notePermissionBtn = view.findViewById(R.id.note_permission_banner_btn)
        notePermissionCloseImg = view.findViewById(R.id.note_permission_banner_close)

        listNoteBanner = view.findViewById(R.id.list_banner)
        listNoteBannerTv = view.findViewById(R.id.list_banner_text)
        listNoteBannerMore = view.findViewById(R.id.list_banner_more)

        batchActionBar = view.findViewById(R.id.batch_action_bar)
        batchActionMarkRead = view.findViewById(R.id.batch_action_mark_read)
        batchActionMarkUnread = view.findViewById(R.id.batch_action_mark_unread)
        batchActionDelete = view.findViewById(R.id.batch_action_delete)
        batchActionMarkRead.setOnClickListener { onBatchMarkReadClicked(true) }
        batchActionMarkUnread.setOnClickListener { onBatchMarkReadClicked(false) }
        batchActionDelete.setOnClickListener { onBatchDeleteClicked() }

        batchTopBar = view.findViewById(R.id.batch_top_bar)
        batchTopBarClose = view.findViewById(R.id.batch_top_bar_close)
        batchTopBarTitle = view.findViewById(R.id.batch_top_bar_title)
        batchTopBarSelectAll = view.findViewById(R.id.batch_top_bar_select_all)
        batchTopBarClose.setOnClickListener { exitSelectionMode() }
        batchTopBarSelectAll.setOnClickListener { toggleSelectAll() }
        // 输入框获取焦点时显示取消按钮
        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchCancelBtn.visibility = View.VISIBLE
            }
        }
        // 点击取消按钮
        searchCancelBtn.setOnClickListener {
            searchEditText.clearFocus()
            searchEditText.setText("")
            searchCancelBtn.visibility = View.GONE
            // 隐藏键盘
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
            presenter.searchIfChanged("")
        }
        // 输入框点击键盘搜索
        searchEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val key = searchEditText.text.toString()
                presenter.searchIfChanged(key)
                // 隐藏键盘
                val imm =
                    context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                searchEditText.clearFocus()
                return@OnEditorActionListener true
            }
            false
        })
    }


    /**
     * 初始化banner的显示
     */
    private fun refreshBanner() {
        //如果是非厂商通道，并且没有忽略电池优化，就提醒用户关闭电池优化
        if (DeviceUtils.getPlatform() == DevicePlatform.Android) {
            if (!DeviceUtils.isIgnoringBatteryOptimizations()) {
                batteryBanner.visibility = View.VISIBLE
                bannerBtn.setOnClickListener {
                    WxpJumpPageUtils.jumpToSystemIgnoreBatteryOptimizationSettings(
                        activity
                    )
                }
                bannerCloseImg.setOnClickListener {
                    batteryBanner.visibility = View.GONE
                }
            } else {
                batteryBanner.visibility = View.GONE
            }
        } else {
            batteryBanner.visibility = View.GONE
        }
    }

    /**
     * 刷新没有权限时候的消息banner提醒
     */
    private fun refreshNotePermissionBanner() {
        val hasNotePermission = PermissionUtils.hasNotificationPermission(activity)
        if (hasNotePermission) {
            //在消息权限正确的前提下，检查是否可以接收到消息
            notePermissionBanner.visibility = View.GONE
            presenter.fetchCheckReason()
        } else {
            notePermissionBanner.setOnClickListener(null)
            notePermissionBanner.visibility = View.VISIBLE
            notePermissionTv.visibility = View.VISIBLE
            notePermissionTv.text = resources.getString(R.string.note_permission_banner_text)
            notePermissionBtn.text = "去打开"
            notePermissionBtn.visibility = View.VISIBLE
            notePermissionBtn.setOnClickListener {
                if (activity is WxpMainActivity) {
                    val mainActivity = activity as WxpMainActivity
                    mainActivity.permissionRequest()
                }
            }
            notePermissionCloseImg.setImageResource(R.drawable.ic_close)
            notePermissionCloseImg.setOnClickListener {
                notePermissionBanner.visibility = View.GONE
                //关闭banner，在坚持后端接收状态
                presenter.fetchCheckReason()
            }
        }
    }

    private fun setupRefreshLayout() {
        // 设置下拉刷新监听器
        refreshLayout.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                val scene = if (openAppFirstRefresh) {
                    WxpMessageListReq.SceneAutoRefresh
                } else {
                    WxpMessageListReq.SceneManual
                }
                presenter.refresh(scene)
                // 刷新一次以后，就不再是打开app第一次刷新了
                openAppFirstRefresh = false
            }
        })

        // 设置刷新头
        refreshHeader = ClassicsHeader(context)
        refreshHeader.setLastUpdateText(presenter.getTipsOfLastRefreshTime())
        refreshLayout.setRefreshHeader(refreshHeader)

        // 设置刷新尾（用于加载更多）
        val refreshFooter = ClassicsFooter(context)
        refreshLayout.setRefreshFooter(refreshFooter)

        // 设置加载更多监听器
        refreshLayout.setOnLoadMoreListener { refreshLayout ->
            presenter.loadMore()
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageListAdapter { message ->
            openMessage(message)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun openMessage(message: WxpMessageListMessage) {
        // 点击消息项，打开网页
        val urlString = message.url.trim()
        if (urlString.isNotEmpty()) {
            WxpJumpPageUtils.jumpToWebUrl(urlString, activity)
            // 标记消息为已读
            message.read = true
            adapter.notifyDataSetChanged()
        }

    }

    override fun showMessageRefreshing(refreshing: Boolean) {
        if (!refreshing) {
            refreshLayout.finishRefresh()
            loadDataFinish()
        }
    }

    override fun showMessageMoreLoading(loading: Boolean, hasMore: Boolean) {
        if (!loading) {
            refreshLayout.finishLoadMore()
        }
        // 如果没有更多数据，禁用加载更多
        refreshLayout.setNoMoreData(!hasMore)
    }

    override fun onMessageList(data: List<WxpMessageListMessage>) {
        messageList.clear()
        messageList.addAll(data)
        adapter.notifyDataSetChanged()
        refreshHeader.setLastUpdateText(presenter.getTipsOfLastRefreshTime())
        loadDataFinish()
    }

    override fun onFeedback() {
        // 震动反馈
        DeviceUtils.vibrator()
    }

    override fun onOpenSubscribeManagerPage(url: String) {
        activity?.let {
            WxpWebViewActivity.start(it, url)
        }
    }

    override fun createPresenter(): IWxpMessageListPresenter {
        return WxpMessageListPresenter(this)
    }

    private fun loadDataFinish() {
        refreshLayout.finishRefresh()
        // 显示/隐藏空状态
        emptyText.visibility = if (messageList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onCheckReason(data: WxpCheckAppMsgReasonResp?) {
        if (data == null || data.code <= 0) {
            notePermissionBanner.visibility = View.GONE
            return
        }
        notePermissionBanner.visibility = View.VISIBLE
        notePermissionTv.visibility = View.VISIBLE
        notePermissionTv.text = data.reason
        notePermissionBtn.visibility = View.GONE

        view?.let {
            val drawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_forward)
            val colorControlNormal = MaterialColors.getColor(it, attr.colorControlNormal)
            drawable?.setTint(colorControlNormal)
            notePermissionCloseImg.setImageDrawable(drawable)
        }
        notePermissionBanner.setOnClickListener {
            WxpJumpPageUtils.jumpToWebUrl(
                url = WxpConfig.appFeUrl + "/app/?code=${data.code}#/no-message",
                activity = activity
            )
        }

    }

    override fun onListBanner(data: WxpListBannerResp?) {
        if (data == null) {
            listNoteBanner.visibility = View.GONE
            return
        }
        listNoteBanner.visibility = View.VISIBLE
        listNoteBannerTv.text = data.title
        listNoteBannerMore.visibility = View.VISIBLE
        listNoteBanner.setOnClickListener {
            val params = WxpDialogParams()
            params.title = data.title
            params.message = data.desc
            params.leftText = "不再显示"
            params.leftBlock = { presenter.closeListBanner(data.id) }
            if (data.url.isNullOrEmpty()) {
                params.rightText = "我知道了"
            } else {
                params.rightText = "查看详情"
                params.rightBlock = {
                    WxpJumpPageUtils.jumpToWebUrl(url = data.url!!, activity = activity)
                }
            }
            WxpDialogUtils.showDialog(params)
        }
    }

    /**
     * 消息列表适配器
     */
    private inner class MessageListAdapter(
        private val onItemClick: (WxpMessageListMessage) -> Unit
    ) : RecyclerView.Adapter<MessageListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(messageList[position])
        }

        override fun getItemCount(): Int = messageList.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val selectCheckbox: CheckBox = itemView.findViewById(R.id.select_checkbox)
            private val unreadDot: View = itemView.findViewById(R.id.unread_dot)
            private val messageTitle: TextView = itemView.findViewById(R.id.message_title)
            private val linkIcon: View = itemView.findViewById(R.id.link_icon)
            private val sourceLabel: TextView = itemView.findViewById(R.id.source_label)
            private val dateLabel: TextView = itemView.findViewById(R.id.date_label)

            fun bind(message: WxpMessageListMessage) {
                messageTitle.text = message.summary
                sourceLabel.text = "来源: ${message.name ?: "未知"}"
                dateLabel.text = WxpDateTimeUtils.formatDateTime(message.createTime)

                // 设置未读状态
                unreadDot.visibility = if (message.read) View.INVISIBLE else View.VISIBLE

                // 设置链接图标
                val sourceUrl = message.sourceUrl?.trim() ?: ""
                val showLink = sourceUrl.isNotEmpty()
                linkIcon.visibility = if (showLink && !selectionMode) View.VISIBLE else View.GONE

                if (showLink) {
                    linkIcon.setOnClickListener {
                        WxpJumpPageUtils.jumpToWebUrl(sourceUrl, activity)
                    }
                }

                // 多选模式下的复选框状态
                if (selectionMode) {
                    selectCheckbox.visibility = View.VISIBLE
                    selectCheckbox.isChecked = message.messageId in selectedIds
                } else {
                    selectCheckbox.visibility = View.GONE
                    selectCheckbox.isChecked = false
                }

                // 设置点击事件
                itemView.setOnClickListener {
                    if (selectionMode) {
                        toggleItemSelection(message)
                    } else {
                        onItemClick(message)
                    }
                }
                //长按事件
                itemView.setOnLongClickListener {
                    if (selectionMode) {
                        // 多选模式下长按也切换选择
                        toggleItemSelection(message)
                        return@setOnLongClickListener true
                    }
                    val readOptText = if (message.read) {
                        "标记未读"
                    } else {
                        "标记已读"
                    }
                    val readOpt = ActionSheetItem(readOptText, {
                        presenter.markMessageReadStatus(message.messageId, !message.read)
                    })
                    val removeOpt = ActionSheetItem("删除", {
                        presenter.deleteById(message.messageId)
                    })
                    val multiSelectOpt = ActionSheetItem(
                        getString(R.string.message_list_batch_select),
                        {
                            enterSelectionMode(message.messageId)
                        }
                    )
                    val actionList = listOf(
                        listOf(readOpt, removeOpt, multiSelectOpt)
                    )
                    activity?.let {
                        ActionSheetDialogFragment(actionList).show(
                            it.supportFragmentManager,
                            "${MessageListFragment::class.simpleName}_option"
                        )
                    }
                    return@setOnLongClickListener true
                }
            }
        }
    }

    // 实现ITabMenuProvider接口
    override fun getMenuResId(): Int {
        return R.menu.menu_message_list
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_subscription_manager -> {
                // 打开订阅管理页面
                presenter.openSubscribeManagerPage()
                true
            }

            R.id.menu_scan_subscribe -> {
                // 扫码添加订阅 - 可以实现扫码功能或跳转到添加订阅页面
                WxpJumpPageUtils.jumpToScan(requireActivity())
                true
            }

            R.id.menu_mark_all_read -> {
                // 标记所有消息为已读
                presenter.markMessageReadStatus(null, true)
                true
            }

            R.id.menu_batch_select -> {
                enterSelectionMode(null)
                true
            }

            R.id.menu_delete_all -> {
                presenter.deleteAll()
                true
            }

            else -> false
        }
    }

    // ---------- 多选模式 ----------

    private fun enterSelectionMode(preselectId: Long?) {
        if (selectionMode) {
            if (preselectId != null) {
                // 已经在多选模式，追加预选
                toggleSelection(preselectId)
            }
            return
        }
        selectionMode = true
        selectedIds.clear()
        preselectId?.let { selectedIds.add(it) }
        refreshLayout.isEnabled = false
        batchTopBar.visibility = View.VISIBLE
        // 多选态隐藏 banner 与搜索栏，让顶部工具栏完整呈现
        view?.findViewById<View>(R.id.banner)?.visibility = View.GONE
        searchBarContainer.visibility = View.GONE
        batchActionBar.visibility = View.VISIBLE
        updateSelectionTitle()
        updateBatchActionBarState()
        adapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        if (!selectionMode) {
            return
        }
        selectionMode = false
        selectedIds.clear()
        refreshLayout.isEnabled = true
        batchTopBar.visibility = View.GONE
        // 恢复 banner 与搜索栏，banner 的子卡片仍按自身 visibility 展示
        view?.findViewById<View>(R.id.banner)?.visibility = View.VISIBLE
        searchBarContainer.visibility = View.VISIBLE
        batchActionBar.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    /**
     * 切换单条消息的选中状态（由 itemView 点击触发）
     */
    private fun toggleItemSelection(message: WxpMessageListMessage) {
        val messageId = message.messageId
        if (messageId in selectedIds) {
            selectedIds.remove(messageId)
        } else {
            if (selectedIds.size >= MAX_BATCH_SELECT) {
                WxpToastUtils.showToast(getString(R.string.message_list_action_limit_toast))
                return
            }
            selectedIds.add(messageId)
        }
        updateSelectionTitle()
        updateBatchActionBarState()
        adapter.notifyDataSetChanged()
    }

    /**
     * 切换选中状态（按 id，用于 preselect 场景）
     */
    private fun toggleSelection(messageId: Long) {
        if (messageId in selectedIds) {
            selectedIds.remove(messageId)
        } else {
            if (selectedIds.size >= MAX_BATCH_SELECT) {
                WxpToastUtils.showToast(getString(R.string.message_list_action_limit_toast))
                return
            }
            selectedIds.add(messageId)
        }
        updateSelectionTitle()
        updateBatchActionBarState()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectionTitle() {
        batchTopBarTitle.text = getString(
            R.string.message_list_action_selected_count,
            selectedIds.size
        )
        val allSelected = messageList.isNotEmpty() &&
            (selectedIds.size >= messageList.size || selectedIds.size >= MAX_BATCH_SELECT)
        batchTopBarSelectAll.text = getString(
            if (allSelected) R.string.message_list_action_deselect_all
            else R.string.message_list_action_select_all
        )
    }

    private fun updateBatchActionBarState() {
        val enabled = selectedIds.isNotEmpty()
        batchActionMarkRead.isEnabled = enabled
        batchActionMarkUnread.isEnabled = enabled
        batchActionDelete.isEnabled = enabled
        val alpha = if (enabled) 1.0f else 0.4f
        batchActionMarkRead.alpha = alpha
        batchActionMarkUnread.alpha = alpha
        batchActionDelete.alpha = alpha
    }

    /**
     * 顶部栏"全选/取消全选"切换
     */
    private fun toggleSelectAll() {
        val allSelected = messageList.isNotEmpty() &&
            (selectedIds.size >= messageList.size || selectedIds.size >= MAX_BATCH_SELECT)
        if (allSelected) {
            deselectAll()
        } else {
            selectAll()
        }
    }

    /**
     * 全选 - 超过 200 只选前 200 条
     */
    private fun selectAll() {
        selectedIds.clear()
        val overLimit = messageList.size > MAX_BATCH_SELECT
        val targets = if (overLimit) {
            messageList.subList(0, MAX_BATCH_SELECT)
        } else {
            messageList
        }
        targets.forEach { selectedIds.add(it.messageId) }
        if (overLimit) {
            WxpToastUtils.showToast(getString(R.string.message_list_action_limit_toast))
        }
        updateSelectionTitle()
        updateBatchActionBarState()
        adapter.notifyDataSetChanged()
    }

    private fun deselectAll() {
        selectedIds.clear()
        updateSelectionTitle()
        updateBatchActionBarState()
        adapter.notifyDataSetChanged()
    }

    private fun onBatchMarkReadClicked(read: Boolean) {
        if (selectedIds.isEmpty()) {
            WxpToastUtils.showToast(getString(R.string.message_list_action_none_selected))
            return
        }
        val ids = selectedIds.toList()
        presenter.markMessageReadStatusBatch(ids, read)
        exitSelectionMode()
    }

    private fun onBatchDeleteClicked() {
        if (selectedIds.isEmpty()) {
            WxpToastUtils.showToast(getString(R.string.message_list_action_none_selected))
            return
        }
        val ids = selectedIds.toList()
        // 用户点击确认后再退出多选态，确认弹窗期间仍能看到已选条目
        presenter.deleteByIds(ids) {
            exitSelectionMode()
        }
    }

}
