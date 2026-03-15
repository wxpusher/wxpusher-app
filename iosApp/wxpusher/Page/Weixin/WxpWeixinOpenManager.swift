//
//  WxpWeixinOpenManager.swift
//  wxpusher
//
//  Created by zjie on 2025/10/8.
//

import Foundation

/// 微信管理单例类
class WxpWeixinOpenManager: NSObject {
    
    // MARK: - 单例实例
    static let shared = WxpWeixinOpenManager()
    
    // MARK: - 属性
    private var appId: String = ""
    private var universalLink: String = ""
    
    /// 微信授权登录回调
    var authCompletion: ((Result<WeChatAuthResponse, WeChatError>) -> Void)?
    
    /// 微信分享回调
    var shareCompletion: ((Result<Bool, WeChatError>) -> Void)?
    
    /// 微信支付回调
    var payCompletion: ((Result<Bool, WeChatError>) -> Void)?
    
    // MARK: - 初始化
    private override init() {
        super.init()
    }
    
    // MARK: - 配置方法
    /// 配置微信SDK 初始化
    func doInit() {
        self.appId = "wx906c2d32c1b1a115"
        self.universalLink = "https://wxpusher.zjiecode.com/launch-app/"
       

        // 设置微信SDK日志级别
        WXApi.startLog(by: .detail) { log in
            print("WeChatSDK: \(log)")
        }
        // 注册微信SDK
        let isRegistered = WXApi.registerApp(appId, universalLink: universalLink)
        print("微信SDK注册结果: \(isRegistered ? "成功" : "失败")")
    }
    
    /// 检查是否安装微信
    func isWeChatInstalled() -> Bool {
        return WXApi.isWXAppInstalled()
    }
    
    /// 检查微信支持版本
    func isWeChatSupport() -> Bool {
        return WXApi.isWXAppSupport()
    }
    
    /// 获取微信版本号
    func getWeChatVersion() -> String {
        return WXApi.getWXAppInstallUrl()
    }
}

// MARK: - WXApiDelegate 协议实现
extension WxpWeixinOpenManager: WXApiDelegate {
    
    /// 收到微信的请求
    func onReq(_ req: BaseReq) {
        print("收到微信请求: \(req)")
        
        if let authReq = req as? SendAuthReq {
            print("收到授权请求: \(authReq)")
        }
    }
    
    /// 收到微信的响应
    func onResp(_ resp: BaseResp) {
        print("收到微信响应: \(resp), errCode: \(resp.errCode), errStr: \(resp.errStr)")
        
        switch resp {
        case let authResp as SendAuthResp:
            handleAuthResponse(authResp)
            
        case let messageResp as SendMessageToWXResp:
            handleShareResponse(messageResp)
            
        case let payResp as PayResp:
            handlePayResponse(payResp)
            
        default:
            print("未知类型的微信响应")
        }
    }
}

// MARK: - 授权登录相关
extension WxpWeixinOpenManager {
    
    /// 发起微信授权登录
    /// - Parameters:
    ///   - scope: 授权范围，默认snsapi_userinfo
    ///   - state: 状态参数
    ///   - completion: 完成回调
    func requestAuth(scope: String = "snsapi_userinfo",
                    state: String = "wechat_auth",
                    completion: @escaping (Result<WeChatAuthResponse, WeChatError>) -> Void) {
        
        guard isWeChatInstalled() else {
            completion(.failure(.notInstalled))
            return
        }
        
        guard isWeChatSupport() else {
            completion(.failure(.unsupportedVersion))
            return
        }
        
        self.authCompletion = completion
        
        let req = SendAuthReq()
        req.scope = scope
        req.state = state
        
        WXApi.send(req) { success in
            if !success {
                completion(.failure(.sendRequestFailed))
            }
        }
    }
    
    /// 处理授权响应
    private func handleAuthResponse(_ response: SendAuthResp) {
        let result: Result<WeChatAuthResponse, WeChatError>
        
        if response.errCode == 0, let code = response.code {
            let authResponse = WeChatAuthResponse(
                code: code,
                state: response.state,
                country: response.country,
                lang: response.lang
            )
            result = .success(authResponse)
        } else {
            let error = WeChatError.fromErrorCode(response.errCode, errorMessage: response.errStr)
            result = .failure(error)
        }
        
        DispatchQueue.main.async {
            self.authCompletion?(result)
            self.authCompletion = nil
        }
    }
}

// MARK: - 分享相关
extension WxpWeixinOpenManager {
    
    /// 分享文本到微信
    func shareText(_ text: String, to scene: WXScene, completion: @escaping (Result<Bool, WeChatError>) -> Void) {
        let req = SendMessageToWXReq()
        req.bText = true
        req.text = text
        req.scene = Int32(scene.rawValue)
        
        shareCompletion = completion
        WXApi.send(req)
    }
    
    /// 分享图片到微信
    func shareImage(_ image: UIImage, to scene: WXScene, completion: @escaping (Result<Bool, WeChatError>) -> Void) {
        guard let imageData = image.jpegData(compressionQuality: 0.8) else {
            completion(.failure(.invalidImage))
            return
        }
        
        let imageObject = WXImageObject()
        imageObject.imageData = imageData
        
        let message = WXMediaMessage()
        message.mediaObject = imageObject
        
        let req = SendMessageToWXReq()
        req.bText = false
        req.message = message
        req.scene = Int32(scene.rawValue)
        
        shareCompletion = completion
        WXApi.send(req)
    }
    
    /// 分享网页到微信
    func shareWebPage(
        url: URL,
        title: String,
        description: String,
        thumbImage: UIImage?,
        to scene: WXScene,
        completion: @escaping (Result<Bool, WeChatError>) -> Void
    ) {
        guard isWeChatInstalled() else {
            completion(.failure(.notInstalled))
            return
        }
        
        guard isWeChatSupport() else {
            completion(.failure(.unsupportedVersion))
            return
        }
        
        let webPageObject = WXWebpageObject()
        webPageObject.webpageUrl = url.absoluteString
        
        let message = WXMediaMessage()
        message.mediaObject = webPageObject
        message.title = title
        message.description = description
        if let thumbImage, let thumbData = compressedThumbData(from: thumbImage) {
            message.thumbData = thumbData
        }
        
        let req = SendMessageToWXReq()
        req.bText = false
        req.message = message
        req.scene = Int32(scene.rawValue)
        
        shareCompletion = completion
        WXApi.send(req) { success in
            if !success {
                DispatchQueue.main.async {
                    self.shareCompletion = nil
                    completion(.failure(.sendRequestFailed))
                }
            }
        }
    }
    
    private func compressedThumbData(from image: UIImage) -> Data? {
        let thumbSize = CGSize(width: 150, height: 150)
        UIGraphicsBeginImageContextWithOptions(thumbSize, false, 1.0)
        image.draw(in: CGRect(origin: .zero, size: thumbSize))
        let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        guard let finalImage = resizedImage else {
            return image.jpegData(compressionQuality: 0.7)
        }
        
        let qualities: [CGFloat] = [0.9, 0.7, 0.5, 0.3]
        for quality in qualities {
            if let data = finalImage.jpegData(compressionQuality: quality), data.count <= 32 * 1024 {
                return data
            }
        }
        return finalImage.jpegData(compressionQuality: 0.2)
    }
    
    /// 处理分享响应
    private func handleShareResponse(_ response: SendMessageToWXResp) {
        let result: Result<Bool, WeChatError>
        
        if response.errCode == 0 {
            result = .success(true)
        } else {
            let error = WeChatError.fromErrorCode(response.errCode, errorMessage: response.errStr)
            result = .failure(error)
        }
        
        DispatchQueue.main.async {
            self.shareCompletion?(result)
            self.shareCompletion = nil
        }
    }
}

// MARK: - 支付相关
extension WxpWeixinOpenManager {
    
    /// 发起微信支付
    func requestPayment(with reqDict: [String: Any]?, completion: @escaping (Result<Bool, WeChatError>) -> Void) {
        guard let reqDict = reqDict else {
            completion(.failure(.unknownError("支付参数为空")))
            return
        }
        
        // 验证必要的参数
        guard let prepayId = reqDict["prepayid"] as? String,
              let sign = reqDict["paySign"] as? String,
              let timeStampString = reqDict["timeStamp"] as? String,
              let nonceStr = reqDict["nonceStr"] as? String,
              let partnerId = reqDict["partnerid"] as? String,
              let package = reqDict["package"] as? String,
              let timeStamp = UInt32(timeStampString) else {
            completion(.failure(.unknownError("支付参数格式错误")))
            return
        }
        
        // 检查微信是否已安装和支持
        guard isWeChatInstalled() else {
            completion(.failure(.notInstalled))
            return
        }
        
        guard isWeChatSupport() else {
            completion(.failure(.unsupportedVersion))
            return
        }
        
        let wxPayReq = PayReq()
        wxPayReq.prepayId = prepayId
        wxPayReq.sign = sign
        wxPayReq.timeStamp = timeStamp
        wxPayReq.nonceStr = nonceStr
        wxPayReq.partnerId = partnerId
        wxPayReq.package = package
        
        payCompletion = completion
        
        WXApi.send(wxPayReq) { success in
            if !success {
                DispatchQueue.main.async {
                    completion(.failure(.sendRequestFailed))
                }
            }
        }
    }
    
    /// 处理支付响应
    private func handlePayResponse(_ response: PayResp) {
        let result: Result<Bool, WeChatError>
        
        if response.errCode == 0 {
            result = .success(true)
        } else {
            let error = WeChatError.fromErrorCode(response.errCode, errorMessage: response.errStr)
            result = .failure(error)
        }
        
        DispatchQueue.main.async {
            self.payCompletion?(result)
            self.payCompletion = nil
        }
    }
}

// MARK: - 数据模型
/// 微信授权响应
struct WeChatAuthResponse {
    let code: String
    let state: String?
    let country: String?
    let lang: String?
}

/// 微信支付参数
struct WeChatPaymentParameters {
    let partnerId: String
    let prepayId: String
    let nonceStr: String
    let timeStamp: String
    let package: String
    let sign: String
}

/// 微信场景
enum WXScene: Int {
    case session = 0       // 聊天界面
    case timeline = 1      // 朋友圈
    case favorite = 2      // 收藏
}

/// 微信错误类型
enum WeChatError: Error, LocalizedError {
    case notInstalled
    case unsupportedVersion
    case sendRequestFailed
    case userCancel
    case authDenied
    case invalidImage
    case paymentFailed
    case unknownError(String?)
    
    var errorDescription: String? {
        switch self {
        case .notInstalled: return "未安装微信"
        case .unsupportedVersion: return "微信版本过低"
        case .sendRequestFailed: return "发送请求失败"
        case .userCancel: return "用户取消"
        case .authDenied: return "授权被拒绝"
        case .invalidImage: return "图片无效"
        case .paymentFailed: return "支付失败"
        case .unknownError(let message): return message ?? "未知错误"
        }
    }
    
    static func fromErrorCode(_ code: Int32, errorMessage: String?) -> WeChatError {
        switch code {
        case -2: return .userCancel
        case -4: return .authDenied
        default: return .unknownError(errorMessage)
        }
    }
}
