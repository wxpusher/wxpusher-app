//
//  WxPusherApi.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/4.
//

import Foundation
import Moya

enum WxPusherApi : WxPusherTargetType {
    var deviceId: String{
        return UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
    }
    
    var deviceToken: String{
        return UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_TOKEN) ?? ""
    }
    
    //网络检查
    case alive
    //发送验证码
    case sendVerify(SendVerifyReq)
    //验证码登录
    case verifyCodeLogn(VerifyCodeLoginReq)
    //更新设备信息
    case updateDeviceInfo(UpdateDeviceInfoBean)
    //查询消息列表
    case messageList(Int64)
    //退出登录
    case logout
    //解绑手机号
    case unbind
    
    
    var parameters: Any?{
        switch self {
        case .sendVerify(let phone):
            return phone
        case .verifyCodeLogn(let req):
            return req
        case .messageList(let lastMessageId):
            return lastMessageId
        case .updateDeviceInfo(let body):
            return body
        default:
            break
        }
        //不需要body参数
        return nil
    }
    
    
    var path: String {
        switch self {
        case .alive:
            return "/api/test/health"
        case .sendVerify:
            return "/api/device/send-verify-code"
        case .verifyCodeLogn:
            return "/api/device/verify-code-login"
        case .messageList:
            return "/api/need-login/device/message-list"
        case .updateDeviceInfo:
            return "/api/need-login/device/update-device-info"
        case .logout:
            return "/api/need-login/device/logout"
        case .unbind:
            return "/api/need-login/device/unbind"
        }
    }
    var method: Moya.Method {
        switch self {
        case .alive:
            return .get
        case .sendVerify:
            return .post
        case .verifyCodeLogn:
            return .post
        case .messageList:
            return .get
        case .updateDeviceInfo:
            return .put
        case .logout:
            return .post
        case .unbind:
            return .post
            
        }
    }
    
    var task: Task{
        switch self {
        case .messageList(let lastMessageId):
            return   .requestParameters(parameters: ["lastMessageId":lastMessageId], encoding: URLEncoding.default)
        default:
            break
        }
        
        if let p = self.parameters as? Encodable{
            return .requestJSONEncodable(p)
        }
        return .requestPlain
    }
    
}
