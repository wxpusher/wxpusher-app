//
//  VerifyCodeLoginBean.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/6.
//
//验证码登录请求
struct VerifyCodeLoginReq:Encodable{
    let phone:String
    let code:String
    //没有deviceId ，就走设备注册逻辑
    let deviceId:String
    let deviceName:String
    let pushToken:String
    
    init(phone: String, code: String, deviceId: String, deviceName: String, pushToken: String) {
        self.phone = phone
        self.code = code
        self.deviceId = deviceId
        self.deviceName = deviceName
        self.pushToken = pushToken
    }
}

//验证码登录返回
struct VerifyCodeLoginResp:Decodable{
    //电话号码是否已经注册
    let phoneHasRegister:Bool
    //电话号码没有注册，可以和公众号绑定或者注册，通过这个code可以验证用户手机号，（手机号+验证码的base64编码）
    let phoneVerifyCode:String?
    let deviceToken:String?
    let deviceId:String?
    let uid:String?
    
    init(phoneHasRegister: Bool, phoneVerifyCode: String?, deviceToken: String?, deviceId: String?,uid:String?) {
        self.phoneHasRegister = phoneHasRegister
        self.phoneVerifyCode = phoneVerifyCode
        self.deviceToken = deviceToken
        self.deviceId = deviceId
        self.uid = uid
    }
}


