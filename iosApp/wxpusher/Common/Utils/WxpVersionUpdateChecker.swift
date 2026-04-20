//
//  WxpVersionUpdateUtils.swift
//  wxpusher
//
//  Created by zjie on 2025/7/15.
//

import UIKit
import shared

/**
 * iOS 端应用市场跳转实现。
 * 版本比对/弹窗/节流均由 shared 层 WxpVersionCheckManager 统一负责，
 * 本类仅实现 jumpToMarket：拿到后端下发的 downloadUrl（一般是 itms-apps 链接）后直接打开。
 * downgradeToTbs 字段仅 Android 使用，iOS 侧忽略。
 */
class WxpAppMarketNavigatorIOS: WxpAppMarketNavigator {

    func jumpToMarket(downloadUrl: String, downgradeToTbs: Bool) {
        guard !downloadUrl.isEmpty, let url = URL(string: downloadUrl) else { return }
        DispatchQueue.main.async {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
    }

    func willShowInternalDialog(downgradeToTbs: Bool) -> Bool {
        // iOS 打开 App Store 没有应用内升级弹窗，始终走 shared 的弹窗流程
        return false
    }
}
