package com.smjcco.wxpusher.kmp.page.main.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.WxpDateTimeUtils
import com.smjcco.wxpusher.kmp.base.WxpBaseMvpFragment
import com.smjcco.wxpusher.kmp.page.web.WxpWebViewActivity
import com.smjcco.wxpusher.page.messagelist.IWxpMessageListPresenter
import com.smjcco.wxpusher.page.messagelist.IWxpMessageListView
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import com.smjcco.wxpusher.page.messagelist.WxpMessageListPresenter
import com.smjcco.wxpusher.page.messagelist.WxpMessageListReq

/**
 * 消息列表Fragment
 * 对应iOS中的MessageListViewController
 */
class MessageListFragment : WxpBaseMvpFragment<IWxpMessageListPresenter>(), IWxpMessageListView,
    ITabMenuProvider {

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
    }

    private fun initViews(view: View) {
        refreshLayout = view.findViewById(R.id.refresh_layout)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyText = view.findViewById(R.id.empty_text)
        searchEditText = view.findViewById(R.id.search_edit_text)
        searchCancelBtn = view.findViewById(R.id.search_cancel_btn)
        searchBarContainer = view.findViewById(R.id.search_bar_container)

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
            WxpWebViewActivity.start(requireActivity(), urlString)
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
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(50)
            }
        }
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
            private val unreadDot: View = itemView.findViewById(R.id.unread_dot)
            private val messageTitle: TextView = itemView.findViewById(R.id.message_title)
            private val linkIcon: View = itemView.findViewById(R.id.link_icon)
            private val sourceLabel: TextView = itemView.findViewById(R.id.source_label)
            private val dateLabel: TextView = itemView.findViewById(R.id.date_label)

            fun bind(message: WxpMessageListMessage) {
                messageTitle.text =
                    message.summary + message.summary + message.summary + message.summary + message.summary + message.summary
                sourceLabel.text = "来源: ${message.name ?: "未知"}"
                dateLabel.text = WxpDateTimeUtils.formatDateTime(message.createTime)

                // 设置未读状态
                unreadDot.visibility = if (message.read) View.INVISIBLE else View.VISIBLE

                // 设置链接图标
                val sourceUrl = message.sourceUrl?.trim() ?: ""
                val showLink = sourceUrl.isNotEmpty()
                linkIcon.visibility = if (showLink) View.VISIBLE else View.GONE

                if (showLink) {
                    linkIcon.setOnClickListener {
//                        WebDetailActivity.openUrl(itemView.context, sourceUrl)
                    }
                }

                // 设置点击事件
                itemView.setOnClickListener {
                    onItemClick(message)
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
                // TODO: 实现扫码功能
                true
            }

            R.id.menu_mark_all_read -> {
                // 标记所有消息为已读
                presenter.markMessageReadStatus(null, true)
                true
            }

            else -> false
        }
    }

}
