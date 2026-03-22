import Foundation
import shared

final class WxpGetEnvBaseUrlBridgeHandler {
    func handle(completion: @escaping WxpBridgeCompletion) {
        completion(.ok([
            "apiBaseUrl": WxpConfig.shared.baseUrl,
            "appFeBaseUrl": WxpConfig.shared.appFeUrl
        ]))
    }
}
