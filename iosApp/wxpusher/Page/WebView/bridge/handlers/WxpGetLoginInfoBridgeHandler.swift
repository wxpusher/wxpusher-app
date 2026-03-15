import Foundation
import shared

final class WxpGetLoginInfoBridgeHandler {
    typealias LoginInfoProvider = () -> Any?
    typealias DictionaryBuilder = (_ object: Any) -> [String: Any]

    private let loginInfoProvider: LoginInfoProvider
    private let dictionaryBuilder: DictionaryBuilder

    init(
        loginInfoProvider: @escaping LoginInfoProvider,
        dictionaryBuilder: @escaping DictionaryBuilder
    ) {
        self.loginInfoProvider = loginInfoProvider
        self.dictionaryBuilder = dictionaryBuilder
    }

    func handle(completion: @escaping WxpBridgeCompletion) {
        guard let loginInfo = loginInfoProvider() else {
            completion(.ok([:]))
            return
        }
        completion(.ok(dictionaryBuilder(loginInfo)))
    }
}
