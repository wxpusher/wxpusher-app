import UIKit
import shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    
    var window: UIWindow?
    
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = (scene as? UIWindowScene) else { return }
        //冷启动由订阅链接拉起：先记下来，首页（登录后）再打开；
        //不是订阅链接的交给微信 SDK（开放标签冷启动拉起时 extinfo 从这里进 onReq，再走同样的暂存逻辑）
        if let url = connectionOptions.urlContexts.first?.url,
           !MainTabBarController.handleSubscribeLink(url.absoluteString, openNow: false) {
            WXApi.handleOpen(url, delegate: WxpWeixinOpenManager.shared)
        }
        if let activity = connectionOptions.userActivities.first(where: { $0.activityType == NSUserActivityTypeBrowsingWeb }),
           !MainTabBarController.handleSubscribeLink(activity.webpageURL?.absoluteString, openNow: false) {
            WXApi.handleOpenUniversalLink(activity, delegate: WxpWeixinOpenManager.shared)
        }
        
        #if DEBUG
        let tempWin = ShakeWindow(windowScene: windowScene)
        #else
        let tempWin = UIWindow(windowScene: windowScene)
        #endif
        
        let mainTabBarController = MainTabBarController()
        tempWin.backgroundColor = .systemBackground
        tempWin.rootViewController = mainTabBarController
        tempWin.makeKeyAndVisible()
        self.window = tempWin
        
    }
    
    func sceneDidDisconnect(_ scene: UIScene) {
    }
    
    func sceneDidBecomeActive(_ scene: UIScene) {
        print("[DEBUG] SceneDelegate - sceneDidBecomeActive")
        // APNs 异常状态依赖客户端重新上报恢复；每次进入前台检查，内部按一小时节流。
        WxpAppDataService.shared.reportIOSActiveIfNeeded()
        //版本升级检测（内部有 3 小时节流；冷启、后台切前台都会触发）
        WxpVersionCheckManager.shared.onAppForeground(force: false)
    }
    
    func sceneWillResignActive(_ scene: UIScene) {
    }
    
    func sceneWillEnterForeground(_ scene: UIScene) {
    }
    
    func sceneDidEnterBackground(_ scene: UIScene) {
        
    }
    
    // Universal Links 处理
    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        //统一订阅链接 https://{域名}/api/s/{sk}
        if MainTabBarController.handleSubscribeLink(userActivity.webpageURL?.absoluteString, openNow: true) {
            return
        }
        WXApi.handleOpenUniversalLink(userActivity, delegate:  WxpWeixinOpenManager.shared)
    }
    
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else {
            return
        }
        //落地页「打开 App 订阅」的 scheme wxpusher://wxpusher.smjcco.com/s
        if MainTabBarController.handleSubscribeLink(url.absoluteString, openNow: true) {
            return
        }
        WXApi.handleOpen(url, delegate: WxpWeixinOpenManager.shared)
    }
    
}

#if DEBUG
/// debug的时候，摇一摇，打开测试面板
class ShakeWindow: UIWindow {
    override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            WxpJumpPageUtils.jumpToTestPanel()
        }
        super.motionEnded(motion, with: event)
    }
}
#endif
