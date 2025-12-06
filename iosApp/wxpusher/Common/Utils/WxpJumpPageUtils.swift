import UIKit


import Foundation
import shared

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
    
    public static func runWithRootVC(completion: @escaping (UINavigationController) -> Void){
        runWithWindows {window in
            let rootView = window.rootViewController
            //如果根是navVC，那就压栈
            if(rootView is UINavigationController){
                let rootVC = rootView as! UINavigationController
                completion(rootVC)
            } else if(rootView is UITabBarController){
                let tabVC = rootView as! UITabBarController
                if(tabVC.selectedViewController  is UINavigationController){
                    let rootVC = tabVC.selectedViewController as! UINavigationController
                    completion(rootVC)
                }
            }
        }
        
    }
    
    /**
     * 跳转到登录页面
     */
    @objc public static func jumpToLogin() {
        runWithWindows(){ window in
            window.rootViewController = UINavigationController(rootViewController:  WxpLoginViewController())
        }
    }
    
    /**
     * 跳转到用户隐私协议
     */
    public static func jumpToUserAgreement() {
        runWithWindows(){ window in
            window.rootViewController = UINavigationController(rootViewController:  UserAgreementViewController())
        }
    }
    
    /**
     * 跳转到账号详情页面
     */
    public static func jumpToAccountDetail(){
        runWithRootVC { rootVC in
            let accountDetail = AccountDetailViewController()
            accountDetail.hidesBottomBarWhenPushed = true
            rootVC.pushViewController(accountDetail, animated: true)
        }
    }
    
    /**
     * 跳转到修改手机号页面
     */
    public static func jumpToChangePhone(){
        runWithRootVC { rootVC in
            let vc = WxpChangePhoneViewController()
            vc.hidesBottomBarWhenPushed = true
            rootVC.pushViewController(vc, animated: true)
        }
    }
    
    /**
     * 跳转到删除账号
     */
    public static func jumpToRemoveAccount(){
        runWithRootVC { rootVC in
            let vc = WxpRemoveAccountViewController()
            vc.hidesBottomBarWhenPushed = true
            rootVC.pushViewController(vc, animated: true)
        }
    }
    /**
     * 跳转到主页
     */
    public static func jumpToMain() {
        runWithWindows(){ window in
            window.rootViewController = MainTabBarController()
        }
    }
    
    /**
     * 跳转到选择注册方式或者绑定的页面
     */
    public static func jumpToRegisterOrBind(data: WxpBindPageData) {
        runWithWindows(){ window in
            window.rootViewController = UINavigationController(rootViewController:  WxpRegisterOrBindViewController(bindPageData: data))
        }
    }
    
    /**
     * 跳转到通过绑定码，通过微信公众号绑定的页面
     */
    public static func jumpToMpBind(data: WxpPhoneBind?) {
        runWithWindows(){ window in
            let rootView = window.rootViewController
            guard let phone = data?.phone,
                  let code = data?.code,
                  let phoneVerifyCode = data?.phoneVerifyCode else {
                return
            }
            
            // 跳转到BindPhone页面
            let bindPhoneVC = WxpBindPhoneViewController(
                phone: phone,
                code: code,
                phoneVerifyCode: phoneVerifyCode
            )
            
            //如果根是navVC，那就压栈
            if(rootView is UINavigationController){
                let rootVC = rootView as! UINavigationController
                rootVC.pushViewController(bindPhoneVC, animated: true)
            } else if(rootView is UITabBarController){
                let tabVC = rootView as! UITabBarController
                if(tabVC.selectedViewController  is UINavigationController){
                    let rootVC = tabVC.selectedViewController as! UINavigationController
                    rootVC.pushViewController(bindPhoneVC, animated: true)
                }
            }
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
            let rootView = window.rootViewController
            let webVC = WxpWebViewController(url: url)
            //            let webVC =  WxpFSafariViewController(url: url)
            //            webVC.dismissButtonStyle = .close
            //如果根是navVC，那就压栈
            if(rootView is UINavigationController){
                let rootVC = rootView as! UINavigationController
                rootVC.pushViewController(webVC, animated: true)
            } else if(rootView is UITabBarController){
                let tabVC = rootView as! UITabBarController
                if(tabVC.selectedViewController  is UINavigationController){
                    let rootVC = tabVC.selectedViewController as! UINavigationController
                    webVC.hidesBottomBarWhenPushed = true
                    rootVC.pushViewController(webVC, animated: true)
                    WxpLogUtils.shared.d(tag: "WxPusher", message: "通过消息详情页面tab跳转", throwable: nil)
                }
            }
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
    /**
     * 跳转到扫码页面
     */
    public static func jumpToScan(callback:@escaping WxpQRCodeScanViewController.Callback) {
        runWithWindows(){ window in
            let rootView = window.rootViewController
            let vc = WxpQRCodeScanViewController()
            vc.callback = callback
            //如果根是navVC，那就压栈
            if(rootView is UINavigationController){
                let rootVC = rootView as! UINavigationController
                rootVC.pushViewController(vc, animated: true)
            } else if(rootView is UITabBarController){
                let tabVC = rootView as! UITabBarController
                if(tabVC.selectedViewController  is UINavigationController){
                    let rootVC = tabVC.selectedViewController as! UINavigationController
                    vc.hidesBottomBarWhenPushed = true
                    rootVC.pushViewController(vc, animated: true)
                }
            }
        }
        
    }
}
