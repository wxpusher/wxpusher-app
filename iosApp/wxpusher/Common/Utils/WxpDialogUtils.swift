//
//  WxpPermissionUtils.swift
//  wxpusher
//
//  Created by zjie on 2025/6/24.
//

import UserNotifications
import UIKit


/**
 * 弹窗一个dialog，在swift中可以直接调用，也会在OC中调用，还可能在kt中通过OC的接口进行调用
 */
class WxpDialogUtils: NSObject{
    
    @objc
    public static func showDialog(params: WxpDialogParams){
        let alert = UIAlertController(
            title: params.title,
            message: params.message,
            preferredStyle: .alert
        )
        if(!( params.leftText ?? "").isEmpty){
            alert.addAction(UIAlertAction(title: params.leftText, style: .default, handler: { _ in
                params.leftBlock?()
            }))
        }
        
        if(!( params.rightText ?? "").isEmpty){
            alert.addAction(UIAlertAction(title: params.rightText, style: .default,handler: { _ in
                params.rightBlock?()
            }))
        }
        
        WxpJumpPageUtils.runWithWindows(){ window in
            window.rootViewController?.present(alert, animated: true)
//            let rootVC: UINavigationController = window.rootViewController as! UINavigationController
//            rootVC.present(alert, animated: true)
        }
    }
    
}
