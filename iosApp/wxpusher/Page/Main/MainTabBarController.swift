import UIKit
import Toaster
import shared
class MainTabBarController: UITabBarController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        //没有同意隐私协议
        if(!WxpSaveService.shared.get(key: WxpSaveKey.UserHasAgreement, value: false)){
            WxpJumpPageUtils.jumpToUserAgreement()
            return
        }
        //没有登录
        let deviceToken = WxpAppDataService.shared.getLoginInfo()?.deviceToken ?? ""
        if(deviceToken.isEmpty){
            WxpJumpPageUtils.jumpToLogin()
            return
        }
        navigationItem.largeTitleDisplayMode = .automatic

        setupViewControllers()
//        setupAppearance()
        setupKvListener()
        notificationPermissionRemind()
        setupListenBackToRegisterAPNs()
          
//        self.delegate = self
    }
    
    
    //底部 tab 类型
    private enum MainTab { case messageList, market, extFunc, profile }

    //已应用的 tab 配置快照，用于 diff：仅变化才重建
    private var appliedTabConfig: WxpTabConfig?
    //tab 导航控制器缓存：重建时复用已存在实例，保留其导航栈与状态（关键：不打断压栈中的配置页）
    private var tabControllerCache: [MainTab: UINavigationController] = [:]
    //KV 变更监听 id（保存即广播机制）
    private var kvListenerId: Int32 = -1

    //按配置构建有序 tab：消息列表(必) → 消息市场(可配) → 扩展功能(可配) → 我的(必)
    private func currentTabs(_ config: WxpTabConfig) -> [MainTab] {
        var tabs: [MainTab] = [.messageList]
        if config.market {
            tabs.append(.market)
        }
        if config.extFunc {
            tabs.append(.extFunc)
        }
        tabs.append(.profile)
        return tabs
    }

    private func makeTabNavController(_ tab: MainTab) -> UINavigationController {
        let vc: UIViewController
        let item: UITabBarItem
        switch tab {
        case .messageList:
            vc = MessageListViewController()
            item = UITabBarItem(title: "消息列表",
                                image: UIImage(systemName: "paperplane"),
                                selectedImage: UIImage(systemName: "paperplane.fill"))
        case .market:
            vc = WxpProviderListViewController()
            item = UITabBarItem(title: "消息市场",
                                image: UIImage(systemName: "cloud"),
                                selectedImage: UIImage(systemName: "cloud.fill"))
        case .extFunc:
            vc = WxpExtFuncViewController()
            item = UITabBarItem(title: "扩展功能",
                                image: UIImage(systemName: "square.grid.2x2"),
                                selectedImage: UIImage(systemName: "square.grid.2x2.fill"))
        case .profile:
            vc = WxpProfileViewController()
            item = UITabBarItem(title: "我的",
                                image: UIImage(systemName: "person"),
                                selectedImage: UIImage(systemName: "person.fill"))
        }
        vc.tabBarItem = item
        return UINavigationController(rootViewController: vc)
    }

    //取缓存的导航控制器，没有则新建并缓存
    private func navController(for tab: MainTab) -> UINavigationController {
        if let cached = tabControllerCache[tab] {
            return cached
        }
        let nav = makeTabNavController(tab)
        tabControllerCache[tab] = nav
        return nav
    }

    private func setupViewControllers() {
        applyTabs(WxpTabConfigStore.shared.read(), initial: true)
    }

    //增量重建：复用已存在的 tab 实例，只新增/移除变化的 tab，按 tab 身份保持选中项。
    //复用当前 tab 的导航控制器 → 其已压栈的配置页得以保留，保存即重建时用户无感。
    private func applyTabs(_ config: WxpTabConfig, initial: Bool) {
        let oldTabs = appliedTabConfig.map { currentTabs($0) } ?? []
        let selectedTab: MainTab? = (!initial && selectedIndex >= 0 && selectedIndex < oldTabs.count)
            ? oldTabs[selectedIndex] : nil
        appliedTabConfig = config
        let newTabs = currentTabs(config)
        //丢弃已移除 tab 的缓存实例
        for tab in tabControllerCache.keys where !newTabs.contains(tab) {
            tabControllerCache.removeValue(forKey: tab)
        }
        self.viewControllers = newTabs.map { navController(for: $0) }
        //恢复到原来所在 tab；若该 tab 已被隐藏，回落到消息列表
        if let sel = selectedTab, let idx = newTabs.firstIndex(of: sel) {
            selectedIndex = idx
        } else if !initial {
            selectedIndex = 0
        }
        self.title = viewControllers?[selectedIndex].title
    }

    //注册 KV 变更监听（保存即广播）：tab_config 变化即增量重建，未变不动。
    //回调已由共享层 runAtMainSuspend 切到主线程。
    private func setupKvListener() {
        kvListenerId = WxpSaveService.shared.addListener(listener: { [weak self] key in
            guard let self = self, key == "tab_config" else {
                return
            }
            self.refreshTabsIfConfigChanged()
        })
    }

    private func refreshTabsIfConfigChanged() {
        guard let applied = appliedTabConfig else {
            return
        }
        let latest = WxpTabConfigStore.shared.read()
        if applied.market == latest.market && applied.extFunc == latest.extFunc {
            return
        }
        applyTabs(latest, initial: false)
    }
    
    //没有权限的异常提醒
    private func notificationPermissionRemind(){
        WxpPermissionUtils.requestNotificationPermission { success in
            if(!success){
                let params = WxpDialogParams()
                params.title = "异常提醒"
                params.message = "WxPusher必须要推送权限才能正常工作，请在【设置-WxPusher消息推送平台-通知】打开通知开关"
                params.leftText = "取消"
                params.rightText = "去设置"
                params.rightBlock = {
                    WxpJumpPageUtils.openAppSettings()
                }
                WxpDialogUtils.showDialog(params: params)
            }
        }
    }
    
    private func setupListenBackToRegisterAPNs(){
        //用户感知用户返回前台，检查是否打开了通知权限，如果已经打开，需要进行一次注册，才能获取到APNs token
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleAppActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
    }
    
    //感知app返回到前台了
    @objc func handleAppActive() {
        //没有pushToken，说明用户最开始可能没有给通知栏权限，每次打开app都会进行提醒，用户可能会打开通知权限，因此检查一次，进行注册
        let pushToken = WxpAppDataService.shared.getPushToken()
        if(pushToken == nil || pushToken!.isEmpty){
            //当页面显示的时候，检查权限，进行一次APNs注册，避免去设置页面打开，回来以后，没有触发注册
            UNUserNotificationCenter.current().getNotificationSettings { settings in
                if settings.authorizationStatus == .authorized {
                    DispatchQueue.main.async {
                        UIApplication.shared.registerForRemoteNotifications()
                    }
                    return
                }
            }
            print("在首页注册APNs")
            return
        }
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        if kvListenerId >= 0 {
            WxpSaveService.shared.removeListener(id: kvListenerId)
        }
    }
    
//    private func setupAppearance() {
//        // 设置 TabBar 外观
//        if #available(iOS 15.0, *) {
//            let appearance = UITabBarAppearance()
//            appearance.configureWithOpaqueBackground()
//            tabBar.standardAppearance = appearance
//            tabBar.scrollEdgeAppearance = appearance
//        }
//        
//        // 设置导航栏外观
//        if #available(iOS 15.0, *) {
//            let appearance = UINavigationBarAppearance()
//            appearance.configureWithOpaqueBackground()
//            UINavigationBar.appearance().standardAppearance = appearance
//            UINavigationBar.appearance().scrollEdgeAppearance = appearance
//        }
//    }
}


//extension MainTabBarController: UITabBarControllerDelegate {
//    func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
//        // 如果搜索栏正在激活，阻止切换 Tab，需要先关闭搜索栏，再进行tab切换
//        if let searchController = navigationItem.searchController,
//           searchController.isActive {
//            searchController.dismiss(animated: true) {
//                if let index = tabBarController.viewControllers?.firstIndex(of: viewController) {
//                    tabBarController.selectedIndex = index
//                    tabBarController.title = viewController.title
//                }
//            }
//            return false
//        }
//        return true
//    }
//    
//    //将当前tab容器VC的标题，设置为当前选中的tab 子VC的标题
//    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
//        tabBarController.title = viewController.title
//    }
//}

/// 扩展功能 tab —— 原生 WebView 容器加载 app-fe 的九宫格入口页。
/// 与消息市场 tab（WxpProviderListViewController）结构一致。
///
/// 标题栏右侧固定齿轮(底部标签设置)。父类 WxpWebViewController 用同一个 rightBarButtonItem
/// 槽位管理"网页操作菜单"，会在导航加载、以及 H5 调 setWebOptionMenu/setWebBottomBar 的异步 override 里
/// 反复重设该槽位，从而清掉我们的按钮。因此在父类每个改动时机之后重新贴回齿轮。
class WxpExtFuncViewController: WxpWebViewController {

    private lazy var settingBarButton = UIBarButtonItem(
        image: UIImage(systemName: "gearshape"),
        style: .plain,
        target: self,
        action: #selector(openTabSetting)
    )

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "扩展功能"
        // 默认隐藏底部 webview 操作栏：父类首次 applyWebMenuVisibility 时 url 还是 nil 会默认显示，
        // 直到网页加载/H5 异步 setWebBottomBarVisible(false) 才隐藏，造成打开瞬间闪烁。这里提前置为隐藏。
        setBottomBarVisibleOverride(false)
        applyRightBarButtons()
        loadPage()
    }

    private var pageUrl: String {
        return "\(WxpConfig.shared.appFeUrl)/app#/ext-func"
    }

    // 贴回固定的右侧按钮（仅齿轮，与 Android 保持一致）
    private func applyRightBarButtons() {
        navigationItem.rightBarButtonItems = [settingBarButton]
    }

    @objc func openTabSetting() {
        WxpJumpPageUtils.jumpToWebUrl(url: "\(WxpConfig.shared.appFeUrl)/app/#/tab-setting")
    }

    //覆盖为空，避免网页标题改变影响 tab 标题
    override func setPageTitle(title: String) {
    }

    // 导航加载完成后父类会重设右侧按钮，这里再贴回我们的按钮
    override func updateWebOptionBtnStatus() {
        super.updateWebOptionBtnStatus()
        applyRightBarButtons()
        closeButton.isEnabled = webView?.canGoBack ?? false
    }

    // 以下三个 override：父类实现内部用 DispatchQueue.main.async 重设右侧按钮，
    // 我们同样用 main.async 排在其后重新贴回（主线程 FIFO，保证晚于父类执行）。
    override func setOptionMenuVisibleOverride(_ visible: Bool?) {
        super.setOptionMenuVisibleOverride(visible)
        DispatchQueue.main.async { [weak self] in
            self?.applyRightBarButtons()
        }
    }

    override func setOptionMenuItemsOverride(_ options: Set<String>?) {
        super.setOptionMenuItemsOverride(options)
        DispatchQueue.main.async { [weak self] in
            self?.applyRightBarButtons()
        }
    }

    override func setBottomBarVisibleOverride(_ visible: Bool?) {
        super.setBottomBarVisibleOverride(visible)
        DispatchQueue.main.async { [weak self] in
            self?.applyRightBarButtons()
        }
    }

    override func getLastBtnIcon() -> String {
        return "house"
    }

    @objc func loadPage() {
        guard let url = URL(string: pageUrl) else {
            return
        }
        webView?.load(URLRequest(url: url))
    }

    @objc override func closeButtonTapped() {
        loadPage()
    }
}
