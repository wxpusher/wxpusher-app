import UIKit
import UserNotifications
import Toaster
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    var window: UIWindow?
    
    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        
        let sceneConfig = UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
       sceneConfig.delegateClass = SceneDelegate.self
       return sceneConfig
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        
        //迁移一次iOS的老版本的数据，避免用户重新登录
        WxpAppDataService.shared.mergeIOSData()
        
         //初始化
         WxpConfig.shared.baseUrl = "https://wxpusher.zjiecode.com"
        
        
//        WxpConfig.shared.baseUrl = "http://127.0.0.1:6100"
        
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
        print("[push]-应用存活，收到用户消息的时候，userInfo=\(userInfo)")
        NotificationCenter.default.post(name: notiKey, object: nil, userInfo: userInfo)
        completionHandler(.newData)
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("[push]-apple push token: \(token)")
        WxpAppDataService.shared.savePushToken(pushToken: token)
        updateDeviceInfo()
    }
} 
