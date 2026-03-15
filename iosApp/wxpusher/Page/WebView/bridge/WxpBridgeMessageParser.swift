import Foundation

final class WxpBridgeMessageParser {
    func parse(_ body: Any) -> WxpBridgeRequest? {
        if let mapBody = body as? [String: Any] {
            return parseDictionary(mapBody)
        }
        if let jsonBody = body as? String,
           let data = jsonBody.data(using: .utf8),
           let jsonObj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            return parseDictionary(jsonObj)
        }
        return nil
    }

    private func parseDictionary(_ body: [String: Any]) -> WxpBridgeRequest? {
        guard let action = body["action"] as? String else {
            return nil
        }
        return WxpBridgeRequest(
            action: action,
            data: body["data"] as? [String: Any] ?? [:],
            callbackId: body["callbackId"] as? String
        )
    }
}
