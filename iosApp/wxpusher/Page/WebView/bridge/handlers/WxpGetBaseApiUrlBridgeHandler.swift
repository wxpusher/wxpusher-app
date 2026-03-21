import Foundation
import shared

final class WxpGetBaseApiUrlBridgeHandler {
    func handle(completion: @escaping WxpBridgeCompletion) {
        completion(.ok(["apiUrl": WxpConfig.shared.baseUrl]))
    }
}
