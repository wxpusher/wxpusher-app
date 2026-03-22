import Foundation

final class WxpPayRequestBridgeHandler {
}

extension WxpPayRequestBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        WxpWeixinOpenManager.shared.requestPayment(with: request.data) { result in
            DispatchQueue.main.async {
                let responseData = Self.convertPaymentResultToData(result)
                emitter.sendNativeEvent(action: "payResponse", data: responseData)
                switch result {
                case .success:
                    emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok(responseData))
                case .failure(let error):
                    emitter.sendBridgeCallback(
                        callbackId: request.callbackId,
                        response: WxpBridgeResponse(
                            success: false,
                            data: responseData,
                            error: error.localizedDescription
                        )
                    )
                }
            }
        }
    }

    private static func convertPaymentResultToData(_ result: Result<Bool, WeChatError>) -> [String: Any] {
        switch result {
        case .success(let success):
            return ["success": success, "message": success ? "支付成功" : "支付失败"]
        case .failure(let error):
            return ["success": false, "message": error.localizedDescription]
        }
    }
}
