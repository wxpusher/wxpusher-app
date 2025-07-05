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
        
        
        setupViewControllers()
        setupAppearance()
        notificationPermissionRemind()
          
        self.delegate = self
        
    }
    
    //没有权限的异常提醒
    private func notificationPermissionRemind(){
        WxpPermissionUtils.requestNotificationPermission { success in
            if(!success){
                var params = WxpDialogParameter()
                params.title = "异常提醒"
                params.message = "WxPusher必须要推送权限才能正常工作，请在【设置-WxPusher消息推送平台-通知】打开相关开关"
                params.leftText = "取消"
                params.rightText = "去设置"
                params.rightBlock = {
                    WxpJumpPageUtils.openAppSettings()
                }
                WxpDialogUtils.showConfirmDialog(params: params)
            }
        }
    }
    
    private func setupViewControllers() {
        let messageListVC = MessageListViewController(mainTabVC: self)
        let profileVC = ProfileViewController(mainTabVC: self)
        // 创建导航控制器
        messageListVC.tabBarItem = UITabBarItem(
            title: "消息列表",
            image: UIImage(systemName: "paperplane"),
            selectedImage: UIImage(systemName: "paperplane.fill")
        )
        profileVC.tabBarItem = UITabBarItem(
            title: "我的",
            image: UIImage(systemName: "person"),
            selectedImage: UIImage(systemName: "person.fill")
        )
        
        // 设置视图控制器数组
        let controllers = [messageListVC, profileVC]
        self.viewControllers = controllers
        self.selectedIndex = 0
        self.title = controllers[self.selectedIndex].title
        
    }
    
    private func setupAppearance() {
        // 设置 TabBar 外观
        if #available(iOS 15.0, *) {
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            tabBar.standardAppearance = appearance
            tabBar.scrollEdgeAppearance = appearance
        }
        
        // 设置导航栏外观
        if #available(iOS 15.0, *) {
            let appearance = UINavigationBarAppearance()
            appearance.configureWithOpaqueBackground()
            UINavigationBar.appearance().standardAppearance = appearance
            UINavigationBar.appearance().scrollEdgeAppearance = appearance
        }
    }
}


extension MainTabBarController: UITabBarControllerDelegate {
    func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
        // 如果搜索栏正在激活，阻止切换 Tab，需要先关闭搜索栏，再进行tab切换
        if let searchController = navigationItem.searchController,
           searchController.isActive {
            searchController.dismiss(animated: true) {
                if let index = tabBarController.viewControllers?.firstIndex(of: viewController) {
                    tabBarController.selectedIndex = index
                    tabBarController.title = viewController.title
                }
            }
            return false
        }
        return true
    }
    
    //将当前tab容器VC的标题，设置为当前选中的tab 子VC的标题
    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        tabBarController.title = viewController.title
    }
} 
