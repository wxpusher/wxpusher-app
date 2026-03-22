import Foundation
import shared

final class WxpShowToastBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let msg = request.data["msg"] as? String, !msg.isEmpty else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("msg is empty"))
            return
        }
        WxpToastUtils.shared.showToast(msg: msg)
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
    }
}
