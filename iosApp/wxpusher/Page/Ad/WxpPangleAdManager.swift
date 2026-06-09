//
//  WxpPangleAdManager.swift
//  wxpusher
//
//  穿山甲（CSJ/Pangle）广告 SDK 初始化封装。
//  参考 WxpWeixinOpenManager 的单例 + doInit 模式。
//
//  合规要点：必须在用户同意隐私政策之后再调用 doInit() 初始化 SDK / 加载广告。
//

import UIKit
import AppTrackingTransparency
import BUAdSDK
import shared
#if DEBUG
import BUAdTestMeasurement
#endif

final class WxpPangleAdManager {
    static let shared = WxpPangleAdManager()

    /// 穿山甲「应用 App ID」（与代码位 ID 不同）
    private let appID = "5833310"

    private var started = false

    private init() {}

    /// 穿山甲 SDK 是否在当前环境可用。
    /// 模拟器（Apple Silicon 上以 x86_64 经 Rosetta 翻译运行）会因 SDK 的底层线程态操作
    /// 触发 `thread_set_state is unimplemented` 断言崩溃，故模拟器一律跳过，仅真机生效。
    static var isSupported: Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        return true
        #endif
    }

    /// 初始化穿山甲 SDK。需在用户同意隐私政策后调用，重复调用无副作用。
    func doInit() {
        guard !started else { return }
        started = true

        guard WxpPangleAdManager.isSupported else {
            WxpLogUtils.shared.d(tag: "WxpAd", message: "模拟器环境，跳过穿山甲SDK初始化", throwable: nil)
            return
        }

        #if DEBUG
        // 穿山甲测量/预览工具：需在 SDK 初始化前开启 debugMode，预览功能才生效
        // BUAdTestMeasurementConfiguration 的 +configuration 工厂方法在 Swift 中桥接为 init()
        BUAdTestMeasurementConfiguration().debugMode = true
        #endif

        let configuration = BUAdSDKConfiguration.configuration()
        configuration.appID = appID
        BUAdSDKManager.start(asyncCompletionHandler: { success, error in
            WxpLogUtils.shared.d(
                tag: "WxpAd",
                message: "穿山甲SDK初始化 success=\(success) error=\(error?.localizedDescription ?? "")",
                throwable: nil
            )
        })
    }

    /// 按当前界面外观设置穿山甲广告主题（深/浅色）。需在加载广告前调用，影响后续渲染的广告创意。
    func applyAdTheme(for traitCollection: UITraitCollection) {
        guard WxpPangleAdManager.isSupported else { return }
        let isDark = traitCollection.userInterfaceStyle == .dark
        BUAdSDKManager.setThemeStatus(isDark ? .night : .normal)
    }

    /// 适时请求 IDFA / ATT 授权（不影响广告展示，仅提升变现）。
    func requestTrackingAuthorizationIfNeeded() {
        if #available(iOS 14, *) {
            guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else { return }
            ATTrackingManager.requestTrackingAuthorization { _ in }
        }
    }
}
