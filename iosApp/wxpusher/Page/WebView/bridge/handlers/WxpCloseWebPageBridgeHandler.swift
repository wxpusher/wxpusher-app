import Foundation

final class WxpCloseWebPageBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let webController = context.viewController as? WxpWebViewController else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("web view controller not found"))
            return
        }
        webController.closeWebPage()
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
    }
}
