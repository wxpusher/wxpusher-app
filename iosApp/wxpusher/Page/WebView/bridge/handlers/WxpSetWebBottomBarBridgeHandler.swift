import Foundation

final class WxpSetWebBottomBarBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let webController = context.viewController as? WxpWebViewController else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("web view controller not found"))
            return
        }
        guard request.data.keys.contains("visible") else {
            webController.setBottomBarVisibleOverride(nil)
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
            return
        }
        guard let visible = parseBool(value: request.data["visible"]) else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("visible must be boolean"))
            return
        }
        webController.setBottomBarVisibleOverride(visible)
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
    }

    private func parseBool(value: Any?) -> Bool? {
        if let boolValue = value as? Bool {
            return boolValue
        }
        if let strValue = value as? String {
            if strValue.lowercased() == "true" {
                return true
            }
            if strValue.lowercased() == "false" {
                return false
            }
        }
        if let numberValue = value as? NSNumber {
            return numberValue.intValue != 0
        }
        return nil
    }
}
