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
//        WxpConfig.shared.baseUrl = "https://wxpusher.zjiecode.com"
        WxpConfig.shared.baseUrl = "http://wxpusher.test.zjiecode.com"
//        WxpConfig.shared.baseUrl = "http://10.0.0.11:6100"
//        WxpConfig.shared.baseUrl = "http://127.0.0.1:6100"
        
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
        
//        if let remoteNotification = launchOptions?[.remoteNotification] as? [String: Any] {
//            print("通过远程通知冷启动，notification=\(remoteNotification)")
//            WxpToastUtils.shared.showToast(msg: remoteNotification.description)
//        }
        
        //设置全局主要颜色
        UIView.appearance(whenContainedInInstancesOf: [UIAlertController.self]).tintColor = UIColor.defAccentPrimaryColor
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
        var dialogParams=WxpDialogParameter()
        dialogParams.title = "异常提醒"
        dialogParams.message = "设备注册苹果APNs服务失败，你收不到消息推送 ，原因如下\n\(error.localizedDescription)"
        dialogParams.rightText = "我知道了"
        WxpDialogUtils.showConfirmDialog(params: dialogParams)
    }
   
//    应用前台的时候，会收到消息， 但是不会弹窗提醒
//    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
//        print("[push]-应用存活-前台，收到用户消息的时候，userInfo=\(userInfo)")
//        NotificationCenter.default.post(name: notiKey, object: nil, userInfo: userInfo)
//        completionHandler(.newData)
//    }
    
    
//  通过这个方法，可以让应用在前台的时候也提醒消息，但是有这个方法，上面的方法就不会调用了
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        var userInfo = notification.request.content.userInfo
        print("[push]-应用存活-前台，收到用户消息的时候，userInfo=\(userInfo)")
        //在前台收到消息，消息不是已读状态
        userInfo["read"] = false
        NotificationCenter.default.post(name: WxpCommonNotification.ClickMessageNotification, object: nil, userInfo: userInfo)
        //应用在前台的时候，如何提醒处理收到的消息
        completionHandler([.banner, .sound, .badge])
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        var userInfo = response.notification.request.content.userInfo
        print("[push]-应用存活-点击通知，userInfo=\(userInfo)")
        let url = userInfo["url"] as!  String?
        guard let url = url else { return }
        WxpJumpPageUtils.jumpToWebUrl(url: url)
        //打开的消息 ，标记为已读状态
        userInfo["read"] = true
        WxpLogUtils.shared.d(tag: "WxPusher", message: "发送消息点击事件", throwable: nil)
        MessageListViewController.setClickMessage(message: userInfo)
        NotificationCenter.default.post(name: WxpCommonNotification.ClickMessageNotification, object: nil, userInfo: userInfo)
        completionHandler()
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("[push]-apple push token: \(token)")
        WxpAppDataService.shared.savePushToken(pushToken: token)
        WxpAppDataService.shared.updateDeviceInfo()
    }
    
}
