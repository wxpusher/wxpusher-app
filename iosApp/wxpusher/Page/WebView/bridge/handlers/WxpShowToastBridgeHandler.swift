import Foundation
import shared

final class WxpShowToastBridgeHandler {

    func handle(_ request: WxpBridgeRequest, completion: @escaping WxpBridgeCompletion) {
        guard let msg = request.data["msg"] as? String, !msg.isEmpty else {
            completion(.fail("msg is empty"))
            return
        }
        WxpToastUtils.shared.showToast(msg: msg)
        completion(.ok())
    }
}
