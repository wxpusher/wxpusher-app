import Foundation

final class WxpWebBridgeManager {
    private struct BridgeHandler {
        let requiresWhitelist: Bool
        let handler: WxpBridgeActionHandler
    }

    private let whitelistHosts: Set<String>
    private let parser: WxpBridgeMessageParser
    private let emitter: WxpBridgeEmitter
    private let currentHostProvider: () -> String?
    private var handlers: [String: BridgeHandler] = [:]

    init(
        whitelistHosts: Set<String>,
        parser: WxpBridgeMessageParser,
        emitter: WxpBridgeEmitter,
        currentHostProvider: @escaping () -> String?
    ) {
        self.whitelistHosts = whitelistHosts
        self.parser = parser
        self.emitter = emitter
        self.currentHostProvider = currentHostProvider
    }

    func registerHandler(action: String, requiresWhitelist: Bool, handler: @escaping WxpBridgeActionHandler) {
        handlers[action] = BridgeHandler(requiresWhitelist: requiresWhitelist, handler: handler)
    }

    func onMessage(_ body: Any) {
        guard let request = parser.parse(body) else {
            return
        }
        dispatch(request)
    }

    func sendNativeEvent(action: String, data: [String: Any]) {
        emitter.sendNativeEvent(action: action, data: data)
    }

    private func dispatch(_ request: WxpBridgeRequest) {
        guard let bridgeHandler = handlers[request.action] else {
            emitter.sendBridgeCallback(
                callbackId: request.callbackId,
                response: .fail("unknown action: \(request.action)")
            )
            return
        }
        if bridgeHandler.requiresWhitelist && !isHostInWhitelist(currentHostProvider()) {
            emitter.sendBridgeCallback(
                callbackId: request.callbackId,
                response: .fail("host is not allowed")
            )
            return
        }
        bridgeHandler.handler(request) { [weak self] response in
            self?.emitter.sendBridgeCallback(callbackId: request.callbackId, response: response)
        }
    }

    private func isHostInWhitelist(_ host: String?) -> Bool {
        guard let host else { return false }
        return whitelistHosts.contains(host)
    }
}

