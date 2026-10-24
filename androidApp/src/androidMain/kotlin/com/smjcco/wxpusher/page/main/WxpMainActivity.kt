package com.smjcco.wxpusher.page.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.smjcco.wxpusher.biz.notify.WxpNotifyClickResolver
import com.smjcco.wxpusher.biz.tab.WxpTabConfig
import com.smjcco.wxpusher.biz.tab.WxpTabConfigStore
import com.smjcco.wxpusher.biz.version.WxpVersionCheckManager
import com.smjcco.wxpusher.common.WxpSaveKey
import com.smjcco.wxpusher.page.main.fragment.ITabMenuProvider
import com.smjcco.wxpusher.page.main.fragment.MessageListFragment
import com.smjcco.wxpusher.page.main.fragment.ProfileFragment
import com.smjcco.wxpusher.page.main.fragment.WxpClassFragment
import com.smjcco.wxpusher.page.main.fragment.WxpExtFuncFragment
import com.smjcco.wxpusher.page.main.fragment.WxpProviderListFragment
import com.smjcco.wxpusher.page.web.WxpWebViewFragment
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.push.ws.alert.WsAlertPlayer
import com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveServiceStarter
import com.smjcco.wxpusher.utils.PermissionRequester
import com.smjcco.wxpusher.utils.PermissionUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageHelper

/**
 * app首页 - 使用ViewPager2 + TabLayout实现Tab切换
 */
class WxpMainActivity : WxpBaseActivity(), CurrentTabProvider {

    override fun isFragmentCurrentTab(fragment: Fragment): Boolean {
        return pagerAdapter.getFragmentAt(viewPager.currentItem) == fragment
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: MainPagerAdapter
    private var currentMenuProvider: ITabMenuProvider? = null
    private var currentMenu: Menu? = null

    //底部 tab 的枚举与当前有序列表（按配置动态构建）
    //CLASS 是常驻的第一个 tab，不受服务端 tab 配置影响
    private enum class MainTab { CLASS, MESSAGE_LIST, MARKET, EXT_FUNC, PROFILE }

    private var tabs: List<MainTab> = emptyList()
    //已应用的 tab 配置快照，用于返回主界面时 diff，仅变化才重建
    private var appliedTabConfig: WxpTabConfig? = null
    private var tabMediator: TabLayoutMediator? = null
    //KV 变更监听 id（保存即广播机制）
    private var kvListenerId: Int = -1

    private var permissionRequester: PermissionRequester? = null

    companion object {
        const val INTENT_KEY_URL = "url"
        //开发者传入的原文链接，服务端各推送通道都用这个名字下发
        const val INTENT_KEY_SOURCE_URL = "sourceUrl"

        /**
         * 班级管理系统入口（内部使用），放在第一个 tab 常驻。
         *
         * 注意：wangan.xdyzf.cn 不是 WxPusher 官方域名，因此**不加入** WxpWebHostPolicy 白名单。
         * 非白名单域名会照常显示顶部的「第三方内容」提醒条，这是官方要求保留的提示。
         * 代价是底部浏览器工具栏默认也会出现，这里用 setBottomBarVisibleOverride(false)
         * 单独关掉，不靠改白名单实现。
         */
        const val CLASS_SITE_URL = "https://wangan.xdyzf.cn/"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //定制版：隐藏标题栏，班级页面直接用满整个屏幕（状态栏与 WebView 内容之间不留原生 UI）。
        //必须在 setContentView 之前隐藏，否则会先画出标题栏再收起来，能看到一下闪动。
        supportActionBar?.hide()
        setContentView(R.layout.main_activity)
        // 初始化视图
        initViews()

        if (checkAppStatus()) {
            //检查没有通过，不走后面的构建页面流程了
            finish()
            return
        }
        title = tabTitle(MainTab.CLASS)
        // 设置ViewPager和TabLayout（按 tab 显隐配置动态构建）
        setupTabListeners()
        rebuildTabs(WxpTabConfigStore.read())
        // 注册 KV 变更监听（保存即广播），tab 配置变化即重建
        setupKvListener()

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
        val url = getNotifyClickParam(intent, INTENT_KEY_URL)
        if (url.isNullOrEmpty()) {
            return
        }
        openNotifyClickUrl(url, getNotifyClickParam(intent, INTENT_KEY_SOURCE_URL))
    }

    /**
     * 从通知点击的 intent 里取参数（详情页地址、原文链接），各推送通道放的位置不一样
     */
    private fun getNotifyClickParam(intent: Intent, key: String): String? {
        //WS、华为、荣耀、VIVO 的通知，以及 OPPO 经 WebViewActivity 转过来的
        val extraValue = intent.getStringExtra(key)
        if (!extraValue.isNullOrEmpty()) {
            return extraValue
        }

        //检查deeplink 里面是否有参数，目前魅族系统推送会使用这种方式
        val queryValue = intent.data?.getQueryParameter(key)
        if (!queryValue.isNullOrEmpty()) {
            return queryValue
        }

        //小米推送的消息
        val miPushMessage =
            intent.getSerializableExtra(PushMessageHelper.KEY_MESSAGE) as MiPushMessage?
        return miPushMessage?.extra?.get(key)
    }

    /**
     * 按「点击通知直接打开原文链接」开关，打开详情页或者原文链接
     */
    private fun openNotifyClickUrl(url: String, sourceUrl: String?) {
        val target = WxpNotifyClickResolver.resolve(url, sourceUrl) ?: return
        //原文链接交给系统打开失败（没有能打开的 App）时，改为打开详情页
        WxpJumpPageUtils.jumpToWebUrl(target.url, this, fallbackUrl = target.fallbackUrl)
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
        //左下角悬浮按钮：进入账号与推送设置（原来在底部 Tab 里的「我的」）
        findViewById<View>(R.id.btn_settings).setOnClickListener {
            WxpProfileActivity.start(this)
        }
    }


    /**
     * 定制版只保留「班级」一个页面，底部 Tab 栏整体隐藏。
     *
     * WxPusher 原本的消息列表 / 消息市场 / 扩展功能全部去掉：
     *   - 消息市场是第三方应用的推送市场，学生用不上
     *   - 扩展功能是 WxPusher 的推广位
     *   - 消息列表的内容班级系统里都有（通知中心），点通知也是直接跳网页详情
     * 「我的」（账号信息、推送设置、退出登录）改成右下角悬浮按钮进入独立页面，
     * 不再占用底部空间。登录 WxPusher 账号仍然保留 —— 推送要靠它拿 UID。
     */
    private fun buildTabs(@Suppress("UNUSED_PARAMETER") config: WxpTabConfig): List<MainTab> {
        return listOf(MainTab.CLASS)
    }

    //tab 的标题与图标
    private fun tabTitle(tab: MainTab): String = when (tab) {
        MainTab.CLASS -> "班级"
        MainTab.MESSAGE_LIST -> "消息列表"
        MainTab.MARKET -> "消息市场"
        MainTab.EXT_FUNC -> "扩展功能"
        MainTab.PROFILE -> "我的"
    }

    private fun tabIcon(tab: MainTab): Int = when (tab) {
        MainTab.CLASS -> R.drawable.ic_home
        MainTab.MESSAGE_LIST -> R.drawable.ic_paperplane
        MainTab.MARKET -> R.drawable.ic_cloud
        MainTab.EXT_FUNC -> R.drawable.ic_ext_func
        MainTab.PROFILE -> R.drawable.ic_person
    }

    //监听只注册一次（tab 重建时不重复注册）
    private fun setupTabListeners() {
        viewPager.isUserInputEnabled = false
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    title = it.text
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

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPager.post {
                    updateMenuForCurrentTab(position)
                }
            }
        })
    }

    //（重新）按配置构建 tab 列表与 ViewPager
    private fun rebuildTabs(config: WxpTabConfig) {
        appliedTabConfig = config
        tabs = buildTabs(config)
        pagerAdapter = MainPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        tabMediator?.detach()
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val mainTab = tabs[position]
            tab.text = tabTitle(mainTab)
            tab.setIcon(tabIcon(mainTab))
        }
        tabMediator?.attach()

        // 延迟初始化当前tab的菜单，确保Fragment已创建
        viewPager.post {
            updateMenuForCurrentTab(viewPager.currentItem)
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
        //用户已经看到消息了，WS 的持续提醒（最长 60 秒）就该停下来
        WsAlertPlayer.stopAll()
        PushManager.showOpenNoteRemindSettingDialog(this)
        //显示首页的时候，尝试启动一次保活服务
        KeepWsAliveServiceStarter.start(this)
        //定制版：关闭 WxPusher 的版本检测。
        //否则会提示「发现新版本」，点更新装回官方版，班级入口就没了。
        //版本统一由班委在群里发新包。
        // WxpVersionCheckManager.onAppForeground()
    }

    //注册 KV 变更监听（保存即广播）：tab_config 变化即重建，未变不动。
    //回调已由共享层 runAtMainSuspend 切到主线程。
    private fun setupKvListener() {
        kvListenerId = WxpSaveService.addListener { key ->
            if (key == WxpTabConfigStore.TAB_CONFIG_KEY) {
                refreshTabsIfConfigChanged()
            }
        }
    }

    private fun refreshTabsIfConfigChanged() {
        //定制版：底部 tab 结构固定为「只有班级」，服务端下发的 tab 配置不影响界面。
        //这里直接跳过 —— 否则配置一变就会重建 ViewPager，导致班级 WebView 无谓地重新加载。
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        //定制版没有底部 Tab 兜底，返回键先交给网页：能后退就后退，退不动才退出 App。
        //否则学生在页面里连点两次返回就直接退出，很容易误触。
        //pagerAdapter 是 lateinit：未登录直接 finish 的分支里它还没初始化，要防一下。
        if (::pagerAdapter.isInitialized) {
            val fragment = pagerAdapter.getFragmentAt(viewPager.currentItem)
            if (fragment is WxpWebViewFragment && fragment.onBackPressed()) {
                return
            }
        }
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        WxpLogUtils.flush()
    }

    override fun onDestroy() {
        if (kvListenerId >= 0) {
            WxpSaveService.removeListener(kvListenerId)
            kvListenerId = -1
        }
        super.onDestroy()
    }

    /**
     * ViewPager2适配器
     * 让FragmentStateAdapter来管理Fragment的创建和状态保存/恢复
     */
    private inner class MainPagerAdapter(
        activity: AppCompatActivity
    ) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = tabs.size

        override fun createFragment(position: Int): Fragment {
            return when (tabs[position]) {
                MainTab.CLASS -> WxpClassFragment.newInstance(CLASS_SITE_URL)
                MainTab.MESSAGE_LIST -> MessageListFragment()
                MainTab.MARKET -> WxpProviderListFragment()
                MainTab.EXT_FUNC -> WxpExtFuncFragment()
                MainTab.PROFILE -> ProfileFragment()
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