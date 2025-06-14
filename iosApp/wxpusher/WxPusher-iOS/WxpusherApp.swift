//
//  wxpusherApp.swift
//  wxpusher
//
//  Created by 张杰 on 2023/7/8.
//

//import SwiftUI
//
//var notiData:[AnyHashable : Any]? = nil;
//
//
//@main
//struct WxpusherApp: App {
//    
//    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
//    var body: some Scene {
//        WindowGroup {
//            HomeView()
//                .onAppear(perform: {
//                    // 设置 -1 可以清除应用角标，但不清除通知中心的推送
//                    // 设置 0 会将通知中心的所有推送一起清空掉
//                    UIApplication.shared.applicationIconBadgeNumber = -1
//                    //冷启动的时候 ，等界面显示出来，在发送打开消息的通知
//                    if let n = notiData{
//                        NotificationCenter.default.post(name: notiKey, object: nil,userInfo: n);
//                        notiData = nil;
//                    }
//                })
//        }
//    }
//    
//}
//
//class AppDelegate: NSObject, UIApplicationDelegate,UNUserNotificationCenterDelegate {
//    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
//        print("[push]begin register push")
//        
//        UNUserNotificationCenter.current().getNotificationSettings { settings in
//            if settings.authorizationStatus == .authorized {
//                dispatch_sync_safely_main_queue{
//                    print("[push]-registerForRemoteNotifications")
//                    application.registerForRemoteNotifications()
//                }
//            }
//        }
//        
//        UNUserNotificationCenter.current().delegate = self
//        
////        感觉可能是swiftUI的问题，这个有问题，没有数据
////        if let remoteNotification = launchOptions?[.remoteNotification] as? [String: Any] {
////            print("通过远程通知冷启动，notification=\(remoteNotification)")
////        }
//        
//        return true
//    }
//    
//    func applicationDidFinishLaunching(_ application: UIApplication) {
//        print("[push]-applicationDidFinishLaunching")
//    }
//    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
//        print("[push]-didFailToRegisterForRemoteNotificationsWithError，error=%@",error)
//    }
//    //    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) async -> UIBackgroundFetchResult {
//    //        print("application，didReceiveRemoteNotification1-userInfo=%@",userInfo)
//    //        return UIBackgroundFetchResult.newData
//    //    }
//    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
//        print("应用存活，收到用户消息的时候")
//        print("[push]-application，didReceiveRemoteNotification2-userInfo=%@",userInfo)
//        NotificationCenter.default.post(name: notiKey, object: nil,userInfo: userInfo)
//        completionHandler(.newData);
//    }
//    
//    
//    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
//        // 获取远程通知的设备令牌
//        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
//        print("[push]-apple push token: \(token)")
//        UserDefaults.standard.set(token, forKey: SAVE_KEY_PUSH_TOKEN)
//        //上传一次pushToken
//        updateDeviceInfo()
//    }
//    
//    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
//        print("应用存活，后台时，用户点击处理")
//        let userInfo = response.notification.request.content.userInfo
//        //如果有通知信息，就通过消息发送出去，打开的页面会打开这个消息
//        print("点击消息内容，userInfo=\(userInfo)")
//        //        let url =  userInfo["messageUrl"] as! String
//        //        let url = "http://wxpusher.zjiecode.com/api/message/A6gnYL5XCfJEYML8qQhksUoNNQRIdR20"
//        //        openUrl(urlStr: url)
//        notiData = userInfo;
//        NotificationCenter.default.post(name: notiKey, object: nil,userInfo: userInfo)
//        completionHandler()
//    }
//    
//    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
//        //只会收到回调，不会发出通知
//        let userInfo = notification.request.content.userInfo
//        print("应用存活，前台时，收到消息的时候调用=\(userInfo)")
//        completionHandler([.banner, .sound, .badge])
//    }
//    
//}
