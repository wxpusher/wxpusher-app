//
//  CommonExt.swift
//  wxpusher
//
//  Created by 张杰 on 2023/7/9.
//

import Foundation
import Combine
import Moya

//用于通知的key
let notiKey = NSNotification.Name("smjc_remote_notifacation")

private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
/// 将代码安全的运行在主线程
func dispatch_sync_safely_main_queue(_ block: () -> ()) {
    if Thread.isMainThread {
        block()
    } else {
        DispatchQueue.main.sync {
            block()
        }
    }
}

///获取当前应用版本号
func version() -> String{
    if let identityVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
        return identityVersion
    } else {
        return ""
    }
}

///更新设备信息
func updateDeviceInfo() -> Void {
    let deviceToken = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_TOKEN) ?? ""
    if(deviceToken.isEmpty){
        print("没有deviceToken，说明没有登录，不进行设备push_token更新")
        return
    }
    
    let deviceUuid = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
    if(deviceUuid.isEmpty){
        print("没有deviceUuid，说明没有登录，不进行设备push_token更新")
        return
    }
    let pushToken = UserDefaults.standard.string(forKey: SAVE_KEY_PUSH_TOKEN) ?? ""
    if(pushToken.isEmpty){
        print("没有pushToken，不更新")
        return
    }
    let deviceInfo=UpdateDeviceInfoBean(deviceUuid:deviceUuid,pushToken: pushToken);
    print("开始更新设备信息-deviceUuid=\(deviceInfo)")
    provider.rx.http(.updateDeviceInfo(deviceInfo)).subscribe { (data:BaseResp<Bool>) in
        print("更新设备信息成功")
    } onError: { error in
        print("获取消息列表页面错误,e=" + error.localizedDescription)
    }
}


func openUrl(urlStr: String) {
    let url = URL(string: urlStr)
    if let u = url{
        if(UIApplication.shared.canOpenURL(u)) {
            UIApplication.shared.open(u, options: [UIApplication.OpenExternalURLOptionsKey.universalLinksOnly: true]) { success in
                print("打开url=\(urlStr),result=\(success)")
            }
        }
        else {
            UIApplication.shared.open(url!, options: [:], completionHandler: nil)
        }
    }else{
        
    }
}

func sendNotification(title:String,subTitle:String){
    let content = UNMutableNotificationContent()
    content.title = title
    content.subtitle = subTitle
    content.sound = UNNotificationSound.default
    let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
    // 5. 添加请求到通知中心
    UNUserNotificationCenter.current().add(request) { error in
        if let e = error{
            print("发送通知失败,error=\(e.localizedDescription)")
        }
    }
}
