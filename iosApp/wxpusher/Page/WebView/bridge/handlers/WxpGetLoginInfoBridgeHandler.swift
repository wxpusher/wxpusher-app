import Foundation
import shared

final class WxpGetLoginInfoBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let loginInfo = WxpAppDataService.shared.getLoginInfo() else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok([:]))
            return
        }
        emitter.sendBridgeCallback(
            callbackId: request.callbackId,
            response: .ok(buildDictionary(from: loginInfo))
        )
    }
}
