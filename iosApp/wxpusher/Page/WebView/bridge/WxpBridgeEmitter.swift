import Foundation

final class WxpBridgeEmitter {
    typealias JsEvaluator = (_ js: String) -> Void
    private let evaluator: JsEvaluator

    init(evaluator: @escaping JsEvaluator) {
        self.evaluator = evaluator
    }

    func sendBridgeCallback(callbackId: String?, response: WxpBridgeResponse) {
        guard let callbackId, !callbackId.isEmpty else {
            return
        }
        var callbackBody: [String: Any] = [
            "callbackId": callbackId,
            "success": response.success
        ]
        if let data = response.data {
            callbackBody["data"] = data
        }
        if let error = response.error, !error.isEmpty {
            callbackBody["error"] = error
        }
        guard let callbackJson = serializeToJsonString(callbackBody) else {
            return
        }
        let escapedJson = escapeForSingleQuotedJs(callbackJson)
        let js = "window.dispatchEvent(new CustomEvent('wxpusherBridgeCallback', { detail: '\(escapedJson)' }));"
        evaluate(js)
    }

    func sendNativeEvent(action: String, data: [String: Any]) {
        guard let jsonString = serializeToJsonString(data) else {
            return
        }
        let escapedJson = escapeForSingleQuotedJs(jsonString)
        let js = "window.dispatchEvent(new CustomEvent('nativeEvent_\(action)', { detail: '\(escapedJson)' }));"
        evaluate(js)
    }

    private func evaluate(_ js: String) {
        if Thread.isMainThread {
            evaluator(js)
            return
        }
        DispatchQueue.main.async { [evaluator] in
            evaluator(js)
        }
    }

    private func escapeForSingleQuotedJs(_ source: String) -> String {
        source
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "'", with: "\\'")
    }

    private func serializeToJsonString(_ body: [String: Any]) -> String? {
        guard let jsonData = try? JSONSerialization.data(withJSONObject: body) else {
            return nil
        }
        return String(data: jsonData, encoding: .utf8)
    }
}
