import Foundation

final class WxpOpenUrlBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let url = request.data["url"] as? String, !url.isEmpty else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("url is empty"))
            return
        }
        WxpJumpPageUtils.jumpToWebUrl(url: url)
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok(["opened": true]))
    }
}
