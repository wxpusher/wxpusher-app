import Foundation

final class WxpSetWebOptionMenuBridgeHandler: WxpBridgeActionHandler {
    private let supportedOptionItems: Set<String> = ["copy_link", "weixin_share", "share", "open_browser"]

    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let webController = context.viewController as? WxpWebViewController else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("web view controller not found"))
            return
        }
        let hasVisible = request.data.keys.contains("visible")
        let hasOptions = request.data.keys.contains("options")
        if !hasVisible && !hasOptions {
            webController.setOptionMenuVisibleOverride(nil)
            webController.setOptionMenuItemsOverride(nil)
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
            return
        }
        if hasVisible {
            guard let visible = parseBool(value: request.data["visible"]) else {
                emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("visible must be boolean"))
                return
            }
            webController.setOptionMenuVisibleOverride(visible)
        }
        if hasOptions {
            guard let options = parseOptions(value: request.data["options"]) else {
                emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("options must be string array"))
                return
            }
            let unsupportedOptions = options.filter { !supportedOptionItems.contains($0) }
            if !unsupportedOptions.isEmpty {
                emitter.sendBridgeCallback(
                    callbackId: request.callbackId,
                    response: .fail("unsupported options: \(unsupportedOptions.joined(separator: ","))")
                )
                return
            }
            webController.setOptionMenuItemsOverride(Set(options))
        }
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
    }

    private func parseOptions(value: Any?) -> [String]? {
        guard let value else {
            return nil
        }
        guard let rawList = value as? [Any] else {
            return nil
        }
        var parsed: [String] = []
        for item in rawList {
            guard let text = item as? String else {
                return nil
            }
            parsed.append(text)
        }
        return parsed
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
