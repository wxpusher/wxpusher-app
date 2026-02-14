package com.smjcco.wxpusher.page.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseActivity
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.flush
import com.smjcco.wxpusher.common.WxpSaveKey
import com.smjcco.wxpusher.page.main.fragment.ITabMenuProvider
import com.smjcco.wxpusher.page.main.fragment.MessageListFragment
import com.smjcco.wxpusher.page.main.fragment.ProfileFragment
import com.smjcco.wxpusher.page.main.fragment.WxpProviderListFragment
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveServiceStarter
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageHelper

/**
 * app首页 - 使用ViewPager2 + TabLayout实现Tab切换
 */
class WxpMainActivity : WxpBaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: MainPagerAdapter
    private var currentMenuProvider: ITabMenuProvider? = null
    private var currentMenu: Menu? = null

    private var permissionRequester: PermissionRequester? = null

    companion object {
        const val INTENT_KEY_URL = "url"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        // 初始化视图
        initViews()

        if (checkAppStatus()) {
            //检查没有通过，不走后面的构建页面流程了
            finish()
            return
        }
        title = "消息列表"
        // 设置ViewPager和TabLayout
        setupViewPager()

        //检查是否有权限
        setUpPermissionRequester()


        //处理页面参数
        onIntent(intent)
        addOnNewIntentListener { onIntent(it) }
    }

    fun permissionRequest() {
        if (permissionRequester == null) {
            PermissionUtils.gotoNotificationSettingPage()
        } else {
            permissionRequester?.request {
                if (!it) {
                    WxpToastUtils.showToast("你拒绝了通知权限，将无法给你发送通知提醒")
                }
            }
        }
    }

    private fun setUpPermissionRequester() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        permissionRequester = PermissionRequester(
            this, Manifest.permission.POST_NOTIFICATIONS,
            "需要发送通知权限",
            "WxPusher是一个消息推送平台，当有新消息到达的时候，我们会第一时间给你发送通知，因此需要你授予发送通知的权限，否则我们无法发送消息通知，你可能会因此遗漏消息，是否授予权限？",
            "缺少必须的通知权限",
            "本应用核心功能是发送消息通知，缺少通知权限会导致你无法收到消息通知。\n\n打开方式：点击“去设置”-“通知管理”-打开允许通知"
        ) {
            PermissionUtils.gotoNotificationSettingPage()
        }
    }

    /**
     * 当打开页面，或者收到newIntent的时候，进行调用
     */
    private fun onIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val url = intent.getStringExtra(INTENT_KEY_URL)
        if (!url.isNullOrEmpty()) {
            WxpJumpPageUtils.jumpToWebUrl(url, this)
            return
        }

        //检查deeplink 里面是否有url，目前魅族系统推送会使用这种方式
        val data = intent.data
        if (data != null) {
            val queryUrl = data.getQueryParameter(INTENT_KEY_URL)
            if (queryUrl != null && !queryUrl.isEmpty()) {
                WxpJumpPageUtils.jumpToWebUrl(queryUrl, this)
                return
            }
        }

        //小米推送的消息，如果有 url，直接打开地址
        val miPushMessage =
            intent.getSerializableExtra(PushMessageHelper.KEY_MESSAGE) as MiPushMessage?
        val miPushUrl = miPushMessage?.extra?.get(INTENT_KEY_URL)
        if (!miPushUrl.isNullOrEmpty()) {
            WxpJumpPageUtils.jumpToWebUrl(miPushUrl, this)
        }
    }

    /**
     * 检查状态，没有通过就返回false，不走后面流程
     */
    private fun checkAppStatus(): Boolean {
        //没有同意隐私协议
        if (!WxpSaveService.get(WxpSaveKey.UserHasAgreement, false)) {
            // 打开隐私协议页面
            WxpJumpPageUtils.jumpToUserAgreement(this)
            finish()
            return true;
        }
        //没有登录
        val token = WxpAppDataService.getLoginInfo()?.deviceToken
        if (token.isNullOrEmpty()) {
            WxpJumpPageUtils.jumpToLogin(this)
            return true
        }
        return false
    }

    private fun initViews() {
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
    }


    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

        // 连接TabLayout和ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "消息列表"
                    tab.setIcon(R.drawable.ic_paperplane)
                }

                1 -> {
                    tab.text = "消息市场"
                    tab.setIcon(R.drawable.ic_cloud)
                }

                2 -> {
                    tab.text = "我的"
                    tab.setIcon(R.drawable.ic_person)
                }
            }
        }.attach()

        // 监听Tab切换，更新Toolbar标题和菜单
        tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    title = it.text
                    // 更新菜单
                    updateMenuForCurrentTab(tab.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 不需要处理
            }
        })

        // 监听ViewPager页面切换（处理手势滑动切换）
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 延迟更新菜单，确保Fragment已经加载完成
                viewPager.post {
                    updateMenuForCurrentTab(position)
                }
            }
        })

        // 延迟初始化第一个tab的菜单，确保Fragment已创建
        viewPager.post {
            updateMenuForCurrentTab(0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 清空之前的菜单
        menu.clear()
        currentMenu = menu

        // 根据当前Fragment设置菜单
        currentMenuProvider?.let { menuProvider ->
            val menuResId = menuProvider.getMenuResId()
            if (menuResId != 0) {
                menuInflater.inflate(menuResId, menu)
                menuProvider.onMenuCreated(menu)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 将菜单点击事件委托给当前Fragment处理
        currentMenuProvider?.let { menuProvider ->
            if (menuProvider.onMenuItemSelected(item)) {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 根据当前tab位置更新菜单
     */
    private fun updateMenuForCurrentTab(position: Int) {
        val fragment = pagerAdapter.getFragmentAt(position)
        if (fragment is ITabMenuProvider) {
            currentMenuProvider = fragment
        } else {
            currentMenuProvider = null
        }
        // 重新创建菜单
        invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        PushManager.showOpenNoteRemindSettingDialog(this)
        //显示首页的时候，尝试启动一次保活服务
        KeepWsAliveServiceStarter.start(this)
    }

    override fun onPause() {
        super.onPause()
        WxpLogUtils.flush()
    }

    /**
     * ViewPager2适配器
     * 让FragmentStateAdapter来管理Fragment的创建和状态保存/恢复
     */
    private inner class MainPagerAdapter(
        activity: AppCompatActivity
    ) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MessageListFragment()
                1 -> WxpProviderListFragment()
                2 -> ProfileFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }

        /**
         * 获取指定位置的Fragment实例
         * 用于在菜单更新时获取当前Fragment
         * ViewPager2使用的Fragment tag格式是 "f" + getItemId(position)
         */
        fun getFragmentAt(position: Int): Fragment? {
            return try {
                val fragmentId = getItemId(position)
                // ViewPager2使用的Fragment tag格式是 "f" + fragmentId
                supportFragmentManager.findFragmentByTag("f$fragmentId")
            } catch (e: Exception) {
                null
            }
        }
    }
}