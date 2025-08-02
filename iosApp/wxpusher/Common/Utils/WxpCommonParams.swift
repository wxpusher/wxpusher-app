//
//  WxpCommonParams.swift
//  wxpusher
//
//  Created by zjie on 2025/7/5.
//

import UIKit

class WxpCommonParams: NSObject{
    
    @objc public static func  appVersionName()->String{
        return (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
    }
}

