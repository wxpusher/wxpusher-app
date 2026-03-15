import Foundation

final class WxpPayRequestBridgeHandler {
    typealias PaymentRequester = (_ data: [String: Any], _ completion: @escaping (Result<Bool, WeChatError>) -> Void) -> Void
    typealias ResultConverter = (_ result: Result<Bool, WeChatError>) -> [String: Any]
    typealias EventSender = (_ action: String, _ data: [String: Any]) -> Void

    private let paymentRequester: PaymentRequester
    private let resultConverter: ResultConverter
    private let eventSender: EventSender

    init(
        paymentRequester: @escaping PaymentRequester,
        resultConverter: @escaping ResultConverter,
        eventSender: @escaping EventSender
    ) {
        self.paymentRequester = paymentRequester
        self.resultConverter = resultConverter
        self.eventSender = eventSender
    }

    func handle(_ request: WxpBridgeRequest, completion: @escaping WxpBridgeCompletion) {
        paymentRequester(request.data) { [weak self] result in
            guard let self else {
                completion(.fail("controller released"))
                return
            }
            DispatchQueue.main.async {
                let responseData = self.resultConverter(result)
                self.eventSender("payResponse", responseData)
                switch result {
                case .success:
                    completion(.ok(responseData))
                case .failure(let error):
                    completion(.fail(error.localizedDescription))
                }
            }
        }
    }
}
