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
        notificationPermissionRemind()
        setupListenBackToRegisterAPNs()
          
//        self.delegate = self
        
        //进行一次检查更新的提醒，iOS不主动检查更新
//        WxpVersionUpdateChecker(force: false).checkForUpdate()
    }
    
    
    private func setupViewControllers() {
        let messageListVC = MessageListViewController()
        let providerListVC = WxpProviderListViewController()
        let profileVC = WxpProfileViewController()
        // 创建导航控制器
        messageListVC.tabBarItem = UITabBarItem(
            title: "消息列表",
            image: UIImage(systemName: "paperplane"),
            selectedImage: UIImage(systemName: "paperplane.fill")
        )
        providerListVC.tabBarItem = UITabBarItem(
            title: "消息市场",
            image: UIImage(systemName: "cloud"),
            selectedImage: UIImage(systemName: "cloud.fill")
        )
        profileVC.tabBarItem = UITabBarItem(
            title: "我的",
            image: UIImage(systemName: "person"),
            selectedImage: UIImage(systemName: "person.fill")
        )
        
        // 设置视图控制器数组
        let controllers = [UINavigationController(rootViewController: messageListVC),
                           UINavigationController(rootViewController: providerListVC),
                           UINavigationController(rootViewController: profileVC)]
        
        self.viewControllers = controllers
        self.title = controllers[self.selectedIndex].title
        
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
