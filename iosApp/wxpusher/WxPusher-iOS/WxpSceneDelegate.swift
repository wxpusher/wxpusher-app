import UIKit
import shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    
    var window: UIWindow?
    
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = (scene as? UIWindowScene) else { return }
        
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
        WXApi.handleOpenUniversalLink(userActivity, delegate:  WxpWeixinOpenManager.shared)
    }
    
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else {
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
