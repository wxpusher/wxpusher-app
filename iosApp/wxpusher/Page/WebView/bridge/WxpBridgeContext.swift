import Foundation
import UIKit

@preconcurrency import WebKit

final class WxpBridgeContext {
    weak var webView: WKWebView?
    weak var viewController: UIViewController?
    private var currentUrlSnapshot: URL?

    init(webView: WKWebView?, viewController: UIViewController?) {
        self.webView = webView
        self.viewController = viewController
        self.currentUrlSnapshot = webView?.url
    }

    var currentUrl: URL? {
        currentUrlSnapshot
    }

    var currentHost: String? {
        currentUrl?.host
    }

    func updateCurrentUrl(_ url: URL?) {
        currentUrlSnapshot = url
    }

    func evaluateJavaScript(_ script: String) {
        webView?.evaluateJavaScript(script, completionHandler: nil)
    }
}
