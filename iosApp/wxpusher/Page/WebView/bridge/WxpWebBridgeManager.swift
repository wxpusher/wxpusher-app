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
        registerHandler(action: "getLoginInfo", requiresWhitelist: true, handler: WxpGetLoginInfoBridgeHandler())
        registerHandler(action: "getEnvBaseUrl", requiresWhitelist: true, handler: WxpGetEnvBaseUrlBridgeHandler())
        registerHandler(action: "showToast", requiresWhitelist: true, handler: WxpShowToastBridgeHandler())
        registerHandler(action: "setWebOptionMenu", requiresWhitelist: true, handler: WxpSetWebOptionMenuBridgeHandler())
        registerHandler(action: "setWebBottomBar", requiresWhitelist: true, handler: WxpSetWebBottomBarBridgeHandler())
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
