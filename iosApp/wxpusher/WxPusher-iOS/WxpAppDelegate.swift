import UIKit
import UserNotifications
import Toaster
import shared

private class WxpBaseInfoServiceListenerImpl:IWxpBaseInfoServiceListener{
    /// 获取客户端操作系统平台，供跨平台业务判断运行环境。
    func getClientPlatform() -> String {
        return "iOS"
    }

    /// iOS 仅使用 APNs，因此后端推送路由平台固定为 iOS。
    func getEffectivePushPlatform() -> String {
        return "iOS"
    }
}

private class IWxpAppPageServiceImpl:IWxpAppPageService{
    func jumpToLogin() {
        WxpJumpPageUtils.jumpToLogin()
    }
}

@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    var window: UIWindow?
    
    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        
        let sceneConfig = UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
        sceneConfig.delegateClass = SceneDelegate.self
        return sceneConfig
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        
        //初始化存储
        WxpSaveService.shared.doInit()
        //初始化配置url等
        WxpConfig.shared.doInit()
        // 启动时触发一次 app_fe 版本刷新（内部节流与异常兜底）
        AppFeVersionManager.shared.refreshOnAppLaunch()
        
        //初始化页面信息
        WxpAppPageService.shared.doInit(service: IWxpAppPageServiceImpl())
        //初始化基础设备信息
        WxpBaseInfoService.shared.doInit(listener: WxpBaseInfoServiceListenerImpl())
        //初始化上层注入的UI-Loading
        WxpLoadingUtils.shared.setLoadingImpl(loadingImpl: WxpLoadingService())
        
        //迁移一次iOS的老版本的数据，避免用户重新登录
        WxpAppDataService.shared.doInit()
        WxpAppDataService.shared.mergeIOSData()
        
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
        
        //设置全局主要颜色
        UIView.appearance(whenContainedInInstancesOf: [UIAlertController.self]).tintColor = UIColor.defAccentPrimaryColor
        
        //注册微信open sdk
        WxpWeixinOpenManager.shared.doInit()
        //注册版本升级市场跳转能力（iOS 打开 downloadUrl 即 App Store）
        WxpVersionCheckManager.shared.setNavigator(navigator: WxpAppMarketNavigatorIOS())
        //初始化穿山甲广告 SDK（已在用户同意隐私政策后进入主流程）
        WxpPangleAdManager.shared.doInit()
        return true
    }

    func applicationDidFinishLaunching(_ application: UIApplication) {
        print("[push]-applicationDidFinishLaunching")
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[push]-didFailToRegisterForRemoteNotificationsWithError，error=%@", error)
        var dialogParams=WxpDialogParams()
        dialogParams.title = "异常提醒"
        dialogParams.message = "设备注册苹果APNs服务失败，你收不到消息推送 ，原因如下\n\(error.localizedDescription)"
        dialogParams.rightText = "我知道了"
        WxpDialogUtils.showDialog(params: dialogParams)
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
        // APNs 回调属于后台自动上报，失败不打扰用户，因此传 silent。
        // Kotlin 默认参数不会导出到 Objective-C，这里必须显式传入。
        WxpAppDataService.shared.updateDeviceInfo(platform: nil, silent: true)
    }
    
    func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        return WXApi.handleOpenUniversalLink(userActivity, delegate: WxpWeixinOpenManager.shared)
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        return WXApi.handleOpen(url, delegate: WxpWeixinOpenManager.shared)
    }
    
}
