//
//  WxpPermissionUtils.swift
//  wxpusher
//
//  Created by zjie on 2025/6/24.
//

import UserNotifications
import UIKit
class WxpPermissionUtils: NSObject{
    
    /**
     * 申请推送权限
     */
    public static func requestNotificationPermission(completion: @escaping (Bool) -> Void){
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .authorized {
                completion(true)
                return
            }
            //没有权限，就申请通知权限
            UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
                    if granted {
                        //用户许可通知权限，注册远程通知
                        DispatchQueue.main.async {
                            UIApplication.shared.registerForRemoteNotifications()
                        }
                        completion(true)
                    }else{
                        completion(false)
                    }
                }
        }
    }
}
