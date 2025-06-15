import UIKit
import Toaster
import shared
class MainTabBarController: UITabBarController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViewControllers()
        setupAppearance()
          
        self.delegate = self
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

//将当前tab容器VC的标题，设置为当前选中的tab 子VC的标题
extension MainTabBarController: UITabBarControllerDelegate {
    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        tabBarController.title = viewController.title
    }
} 
