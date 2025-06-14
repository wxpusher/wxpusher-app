import UIKit
import UserNotifications
import Toaster

@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    var window: UIWindow?
    
    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
//        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
        
        let sceneConfig = UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
       sceneConfig.delegateClass = SceneDelegate.self
        
       return sceneConfig
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        print("[push]begin register push")
        // 设置根视图控制器
//        let mainTabBarController = MainTabBarController()
//        let navigationController = UINavigationController(rootViewController: mainTabBarController)
//        // 创建并设置窗口
//        let window = UIWindow(frame: UIScreen.main.bounds)
//        window.backgroundColor = .white
//        window.rootViewController = navigationController
//        window.makeKeyAndVisible()
//        self.window = window
//
//        let vc = UIViewController()
//        let text = UILabel()
//        text.text="这是一个测试文本"
//        text.textColor = .gray
//        vc.view = text
//        let window = UIWindow(frame: UIScreen.main.bounds)
//        window.backgroundColor = .white
//        window.rootViewController = vc
//        window.makeKeyAndVisible()
//        self.window = window
//        print("[UIScreen.main.bounds]=\(UIScreen.main.bounds)")
        
        // 注册推送
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .authorized {
                DispatchQueue.main.async {
                    print("[push]-registerForRemoteNotifications")
                    application.registerForRemoteNotifications()
                }
            }
        }
        
        UNUserNotificationCenter.current().delegate = self
        
        return true
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        print("[DEBUG] AppDelegate - applicationDidBecomeActive")
       
    }
    
    func applicationDidFinishLaunching(_ application: UIApplication) {
        print("[push]-applicationDidFinishLaunching")
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[push]-didFailToRegisterForRemoteNotificationsWithError，error=%@", error)
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        print("应用存活，收到用户消息的时候")
        print("[push]-application，didReceiveRemoteNotification2-userInfo=%@", userInfo)
        NotificationCenter.default.post(name: notiKey, object: nil, userInfo: userInfo)
        completionHandler(.newData)
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("[push]-apple push token: \(token)")
        UserDefaults.standard.set(token, forKey: SAVE_KEY_PUSH_TOKEN)
        updateDeviceInfo()
    }
} 
