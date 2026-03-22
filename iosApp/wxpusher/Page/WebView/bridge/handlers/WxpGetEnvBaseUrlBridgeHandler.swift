import Foundation
import shared

final class WxpGetEnvBaseUrlBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        emitter.sendBridgeCallback(
            callbackId: request.callbackId,
            response: .ok([
                "apiBaseUrl": WxpConfig.shared.baseUrl,
                "appFeBaseUrl": WxpConfig.shared.appFeUrl
            ])
        )
    }
}
