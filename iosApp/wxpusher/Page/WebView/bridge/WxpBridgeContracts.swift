import Foundation

struct WxpBridgeRequest {
    let action: String
    let data: [String: Any]
    let callbackId: String?
}

struct WxpBridgeResponse {
    let success: Bool
    let data: [String: Any]?
    let error: String?

    static func ok(_ data: [String: Any]? = nil) -> WxpBridgeResponse {
        WxpBridgeResponse(success: true, data: data, error: nil)
    }

    static func fail(_ error: String) -> WxpBridgeResponse {
        WxpBridgeResponse(success: false, data: nil, error: error)
    }
}

typealias WxpBridgeCompletion = (WxpBridgeResponse) -> Void
typealias WxpBridgeActionHandler = (_ request: WxpBridgeRequest, _ completion: @escaping WxpBridgeCompletion) -> Void
