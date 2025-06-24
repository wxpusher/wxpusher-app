import UIKit


import Foundation


@objc class WxpJumpPageUtils: NSObject {
    
    /**
     *  拿到一个window再运行block
     */
    public static func runWithWindows(completion: @escaping (UIWindow) -> Void){
        DispatchQueue.main.async {
            guard let window = UIApplication.shared.keyWindow ?? UIApplication.shared.windows.first else {
                return
            }
            completion(window)
        }
        
    }
    
    /**
    * 跳转到登录页面
    */
    @objc public static func jumpToLogin() {
        runWithWindows(){ window in
            let rootVC: UINavigationController = window.rootViewController as! UINavigationController
            // 替换为你的登录VC
            let loginVC = WxpLoginViewController()
            rootVC.setViewControllers([loginVC], animated: false)
        }
    }
    
    /**
     * 跳转到用户隐私协议
     */
     public static func jumpToUserAgreement() {
         runWithWindows(){ window in
             let rootVC: UINavigationController = window.rootViewController as! UINavigationController
             let vc = UserAgreementViewController()
             rootVC.setViewControllers([vc], animated: false)
         }
    }
    /**
     * 跳转到主页
     */
    public static func jumpToMain() {
        runWithWindows(){ window in
            let rootVC: UINavigationController = window.rootViewController as! UINavigationController
            let vc = MainTabBarController()
            rootVC.setViewControllers([vc], animated: false)
        }
   }

    /**
    * 跳转到web页面
    */
    public static func jumpToWebUrl(url: String?) {
        runWithWindows(){ window in
            guard let urlString = url?.trimmingCharacters(in: .whitespaces),
                  !urlString.isEmpty
            else {
                // 处理 URL 为空的情况
                print("URL is empty or nil")
                return
            }

            guard let url = URL(string: urlString) else {
                // 处理 URL 无效的情况
                print("Invalid URL: \(urlString)")
                return
            }


            let webVC = WebViewController(url: url)
            let rootVC = window.rootViewController as! UINavigationController
            rootVC.pushViewController(webVC, animated: true)
        }
       
    }
    
    /**
     * 打开应用设置页面
     */
    public static func openAppSettings() {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        
        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl) { success in
                print("Settings opened: \(success)")
            }
        }else{
            print("Settings Can Not open")
        }
    }
}
