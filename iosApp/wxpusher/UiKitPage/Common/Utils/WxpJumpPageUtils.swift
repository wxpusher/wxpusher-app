import UIKit

class WxpJumpPageUtils{
    /**
     * 跳转到登录页面
     */
    public static func jumpToLogin(){
        DispatchQueue.main.async {
            // 获取keyWindow
            guard let window = UIApplication.shared.keyWindow ?? UIApplication.shared.windows.first else {
                return
            }
            
            // 创建登录ViewController
            let loginVC = WxpLoginViewController() // 替换为你的登录VC
            let rootVC:UINavigationController = window.rootViewController as! UINavigationController
            rootVC.setViewControllers([loginVC], animated: false)
        }
    }
}
