import Foundation

final class WxpOpenUrlBridgeHandler {
    func handle(_ request: WxpBridgeRequest, completion: @escaping WxpBridgeCompletion) {
        guard let url = request.data["url"] as? String, !url.isEmpty else {
            completion(.fail("url is empty"))
            return
        }
        WxpJumpPageUtils.jumpToWebUrl(url: url)
        completion(.ok(["opened": true]))
    }
}
