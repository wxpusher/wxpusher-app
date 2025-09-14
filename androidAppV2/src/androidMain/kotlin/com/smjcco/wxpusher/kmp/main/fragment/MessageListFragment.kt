package com.smjcco.wxpusher.kmp.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.kmp.base.WxpBaseFragment
import com.smjcco.wxpusher.kmp.base.WxpBaseMvpFragment
import com.smjcco.wxpusher.page.messagelist.IWxpMessageListPresenter
import com.smjcco.wxpusher.page.messagelist.IWxpMessageListView
import com.smjcco.wxpusher.page.messagelist.WxpMessageListMessage
import com.smjcco.wxpusher.page.messagelist.WxpMessageListPresenter

/**
 * 消息列表Fragment
 * 对应iOS中的MessageListViewController
 */
class MessageListFragment : WxpBaseMvpFragment<IWxpMessageListPresenter>(), IWxpMessageListView {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageListAdapter

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
        setupRecyclerView()
        presenter.init()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
    }

    private fun setupRecyclerView() {
        adapter = MessageListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun showMessageRefreshing(refreshing: Boolean) {
        TODO("Not yet implemented")
    }

    override fun showMessageMoreLoading(loading: Boolean, hasMore: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onMessageList(data: List<WxpMessageListMessage>) {
        TODO("Not yet implemented")
    }

    override fun onFeedback() {
        TODO("Not yet implemented")
    }

    override fun onOpenSubscribeManagerPage(url: String) {
        TODO("Not yet implemented")
    }

    override fun createPresenter(): IWxpMessageListPresenter {
        return WxpMessageListPresenter(this)
    }


    /**
     * 消息列表适配器
     */
    private inner class MessageListAdapter : RecyclerView.Adapter<MessageListAdapter.ViewHolder>() {

        private val messages = mutableListOf<String>() // TODO: 替换为实际的消息数据模型

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // TODO: 绑定消息数据
            holder.bind(messages[position])
        }

        override fun getItemCount(): Int = messages.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(message: String) {
                // TODO: 绑定消息数据到视图
            }
        }
    }
}
