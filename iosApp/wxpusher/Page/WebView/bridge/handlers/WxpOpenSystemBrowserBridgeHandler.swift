import Foundation
import UIKit

/// 用系统默认浏览器打开 url（跳出 App 到外部浏览器），区别于 openUrl 的内置 WebView
final class WxpOpenSystemBrowserBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let urlString = request.data["url"] as? String,
              !urlString.isEmpty,
              let url = URL(string: urlString) else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("url is empty"))
            return
        }
        DispatchQueue.main.async {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok(["opened": true]))
    }
}
