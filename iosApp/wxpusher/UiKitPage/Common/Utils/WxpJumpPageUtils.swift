import UIKit


import Foundation


@objc class WxpJumpPageUtils: NSObject {
    /**
    * 跳转到登录页面
    */
    @objc public static func jumpToLogin() {
        DispatchQueue.main.async {
            // 获取keyWindow
            guard let window = UIApplication.shared.keyWindow ?? UIApplication.shared.windows.first else {
                return
            }

            // 创建登录ViewController
            let loginVC = WxpLoginViewController() // 替换为你的登录VC
            let rootVC: UINavigationController = window.rootViewController as! UINavigationController
            rootVC.setViewControllers([loginVC], animated: false)
        }
    }

    /**
    * 跳转到web页面
    */
    public static func jumpToWebUrl(url: String?) {
        DispatchQueue.main.async {
            // 获取keyWindow
            guard let window = UIApplication.shared.keyWindow ?? UIApplication.shared.windows.first else {
                return
            }
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
//
//
//            navigationController?.pushViewController(webVC, animated: true)
//
//            // 创建登录ViewController
//            let loginVC = WxpLoginViewController() // 替换为你的登录VC
//            let rootVC:UINavigationController = window.rootViewController as! UINavigationController
//            rootVC.setViewControllers([loginVC], animated: false)
        }
    }
}
