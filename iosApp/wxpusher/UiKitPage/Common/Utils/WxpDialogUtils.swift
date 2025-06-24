//
//  WxpPermissionUtils.swift
//  wxpusher
//
//  Created by zjie on 2025/6/24.
//

import UserNotifications
import UIKit



struct WxpDialogParameter{
    var title: String?
    var message: String?
    var leftText: String?
    var leftBlock: WxpBlockNoParamNoReturn?
    var rightText: String?
    var rightBlock: WxpBlockNoParamNoReturn?
}

class WxpDialogUtils: NSObject{
    
    /**
     * 弹出confirm弹窗
     */
    public static func showConfirmDialog(params: WxpDialogParameter){
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
            let rootVC: UINavigationController = window.rootViewController as! UINavigationController
            rootVC.present(alert, animated: true)
        }
    }
    
}
