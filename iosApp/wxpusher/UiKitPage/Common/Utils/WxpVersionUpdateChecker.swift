//
//  WxpVersionUpdateUtils.swift
//  wxpusher
//
//  Created by zjie on 2025/7/15.
//


import UIKit
import shared

class WxpVersionUpdateChecker {
    
    private let bundleId:String
    init() {
        bundleId = Bundle.main.bundleIdentifier ?? ""
    }
    func checkForUpdate() {
        guard let url = URL(string: "https://itunes.apple.com/lookup?bundleId=\(bundleId)") else {
            return
        }
        
        let task = URLSession.shared.dataTask(with: url) { data, _, error in
            guard let data = data, error == nil else {
                print("Error fetching version: \(error?.localizedDescription ?? "Unknown error")")
                return
            }
            
            do {
                let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
                let results = json?["results"] as? [[String: Any]]
                guard let appStoreVersion = results?.first?["version"] as? String,
                      let trackViewUrl = results?.first?["trackViewUrl"] as? String,
                      let releaseNotes = results?.first?["releaseNotes"] as? String,
                      let currentVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String else {
                    return
                }
                DispatchQueue.main.async {
                    if self.compareVersions(current: currentVersion, appStore: appStoreVersion) {
                        self.showUpdateAlert(appStoreURL: trackViewUrl,releaseNote: releaseNotes)
                    }else{
                        WxpToastUtils.shared.showToast(msg: "当前已是最新版本")
                    }
                }
            } catch {
                WxpToastUtils.shared.showToast(msg: "检查失败\n \(error.localizedDescription)")
            }
        }
        task.resume()
    }
    
    private func compareVersions(current: String, appStore: String) -> Bool {
        let currentComponents = current.components(separatedBy: ".")
        let appStoreComponents = appStore.components(separatedBy: ".")
        
        for i in 0..<max(currentComponents.count, appStoreComponents.count) {
            let currentPart = i < currentComponents.count ? Int(currentComponents[i]) ?? 0 : 0
            let appStorePart = i < appStoreComponents.count ? Int(appStoreComponents[i]) ?? 0 : 0
            
            if appStorePart > currentPart {
                return true
            } else if appStorePart < currentPart {
                return false
            }
        }
        return false
    }
    
    private func showUpdateAlert(appStoreURL: String,releaseNote:String?) {
        let parmas = WxpDialogParameter(title: "有新版本可用",
                                        message: releaseNote ?? "请升级版本以获得最佳体验。",
                                        leftText: "取消",
                                        rightText: "更新") {
            self.openAppStore(appStoreURL: appStoreURL)
        }
        WxpDialogUtils.showConfirmDialog(params: parmas)
    }
    
    private func openAppStore(appStoreURL: String) {
        guard let url = URL(string: appStoreURL) else { return }
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
        //        guard let appStoreURL = URL(string: "itms-apps://itunes.apple.com/app/\(bundleId)") else {
        //            return
        //        }
        //
        //        if UIApplication.shared.canOpenURL(appStoreURL) {
        //            UIApplication.shared.open(appStoreURL, options: [:], completionHandler: nil)
        //        }
    }
}
