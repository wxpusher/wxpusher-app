import Foundation
import shared

final class WxpWebBridgeManager {
    private let context: WxpBridgeContext
    private let parser: WxpBridgeMessageParser
    private let emitter: WxpBridgeEmitter
    private var handlers: [String: WxpRegisteredBridgeHandler] = [:]

    init(
        context: WxpBridgeContext,
        parser: WxpBridgeMessageParser = WxpBridgeMessageParser(),
        emitter: WxpBridgeEmitter
    ) {
        self.context = context
        self.parser = parser
        self.emitter = emitter
        registerDefaultHandlers()
    }

    func onMessage(_ body: Any) {
        guard let request = parser.parse(body) else {
            return
        }
        dispatch(request)
    }

    private func registerDefaultHandlers() {
        registerHandler(action: "payRequest", requiresWhitelist: true, handler: WxpPayRequestBridgeHandler())
        registerHandler(action: "openUrl", requiresWhitelist: false, handler: WxpOpenUrlBridgeHandler())
        registerHandler(action: "openSystemBrowser", requiresWhitelist: true, handler: WxpOpenSystemBrowserBridgeHandler())
        registerHandler(action: "getLoginInfo", requiresWhitelist: true, handler: WxpGetLoginInfoBridgeHandler())
        registerHandler(action: "getEnvBaseUrl", requiresWhitelist: true, handler: WxpGetEnvBaseUrlBridgeHandler())
        registerHandler(action: "showToast", requiresWhitelist: true, handler: WxpShowToastBridgeHandler())
        registerHandler(action: "setWebOptionMenu", requiresWhitelist: true, handler: WxpSetWebOptionMenuBridgeHandler())
        registerHandler(action: "setWebBottomBar", requiresWhitelist: true, handler: WxpSetWebBottomBarBridgeHandler())
        registerHandler(action: "closeWebPage", requiresWhitelist: true, handler: WxpCloseWebPageBridgeHandler())
        registerHandler(action: "getByKey", requiresWhitelist: true, handler: WxpGetByKeyBridgeHandler())
        registerHandler(action: "setKeyValue", requiresWhitelist: true, handler: WxpSetKeyValueBridgeHandler())
    }

    private func registerHandler(action: String, requiresWhitelist: Bool, handler: WxpBridgeActionHandler) {
        handlers[action] = WxpRegisteredBridgeHandler(requiresWhitelist: requiresWhitelist, handler: handler)
    }

    private func dispatch(_ request: WxpBridgeRequest) {
        guard let bridgeHandler = handlers[request.action] else {
            emitter.sendBridgeCallback(
                callbackId: request.callbackId,
                response: .fail("unknown action: \(request.action)")
            )
            return
        }
        if bridgeHandler.requiresWhitelist && !isHostInWhitelist(context.currentHost) {
            emitter.sendBridgeCallback(
                callbackId: request.callbackId,
                response: .fail("host is not allowed")
            )
            return
        }
        bridgeHandler.handler.handle(request: request, context: context, emitter: emitter)
    }

    private func isHostInWhitelist(_ host: String?) -> Bool {
        return WxpWebHostPolicy.shared.isHostInWhitelist(host: host)
    }
}

/// 通用存储读取。直接读 WxpSaveService（原生与 H5 同一 key 空间，无前缀），空值视为不存在返回空 data。
final class WxpGetByKeyBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let key = request.data["key"] as? String, !key.isEmpty else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("key is empty"))
            return
        }
        let value = WxpSaveService.shared.getStringValue(key: key, defaultValue: "")
        if value.isEmpty {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok([:]))
        } else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok(["value": value]))
        }
    }
}

/// 通用存储写入。直接写 WxpSaveService（原生与 H5 同一 key 空间，无前缀）；安全由桥 requiresWhitelist 保证。
final class WxpSetKeyValueBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        guard let key = request.data["key"] as? String, !key.isEmpty else {
            emitter.sendBridgeCallback(callbackId: request.callbackId, response: .fail("key is empty"))
            return
        }
        let value = request.data["value"] as? String ?? ""
        WxpSaveService.shared.setStringValue(key: key, value: value)
        emitter.sendBridgeCallback(callbackId: request.callbackId, response: .ok())
    }
}
