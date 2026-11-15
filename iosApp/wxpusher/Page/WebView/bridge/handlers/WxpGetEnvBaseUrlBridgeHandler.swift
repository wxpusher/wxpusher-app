import Foundation
import shared

final class WxpGetEnvBaseUrlBridgeHandler: WxpBridgeActionHandler {
    func handle(request: WxpBridgeRequest, context: WxpBridgeContext, emitter: WxpBridgeEmitter) {
        emitter.sendBridgeCallback(
            callbackId: request.callbackId,
            response: .ok([
                "apiBaseUrl": WxpConfig.shared.baseUrl,
                "appFeBaseUrl": WxpConfig.shared.appFeUrl,
                // 桥协议 D7：app-fe 内嵌消息详情页时据此传 wxpClient（monorepo docs/message-detail-sandbox-design.md 第 6.10 节）。
                // iOS 的桥只接受主 frame 的调用（WxpWebViewController 的 WKScriptMessageHandler），所以 frameSafe 恒为 true
                "platform": WxpBaseInfoService.shared.getClientPlatform(),
                "versionName": WxpBaseInfoService.shared.getAppVersionName(),
                "frameSafe": true
            ])
        )
    }
}
