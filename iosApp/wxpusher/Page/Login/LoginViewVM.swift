//
//  LoginViewVM.swift
//  WxPusher-iOS
//
//  Created by 张杰 on 2023/10/24.
//

import SwiftUI
import Combine
import Moya
import RxSwift
import Foundation

class LoginViewVM: BaseVM {
    
    @Published var showTipsDialog: Bool = false
    
    @Published var agreePrivacy: Bool = false
    
    @Published var restSecond:Int = 0
    
    @Published var sendLoading:Bool = false
    
    @Published var loginLoading:Bool = false
    
    //跳转到注册页面
    @Published var phoneVerifyCode:String = ""
    @Published var jumpToBindPhoneView:Bool = false
    
    
    @Published var showWebView:Bool = false
    
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    
    var timer:Timer?;
    
    func setShowWebView(show: Bool){
        showWebView = show;
    }
    
    func initIimer() {
        if timer != nil {
            timer!.invalidate()
        }
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] t in
            if((self?.restSecond ?? 0) > 0){
                self?.restSecond -= 1
            }else {
                t.invalidate()
            }
        }
    }
    
    
    func sendVerifyCode(phone:String){
        if phone.isEmpty || phone.count != 11{
            self.homeVM?.showToast(text: "电话号码错误")
            return
        }
        sendLoading=true
        self.provider.rx.http(.sendVerify(SendVerifyReq(phone: phone))).subscribe { [weak self](data:BaseResp<Bool>) in
            self?.homeVM?.showToast(text: "短信已发送")
            self?.sendLoading=false
            self?.restSecond = 120;
            self?.initIimer()
            if let t = self?.timer {
                RunLoop.current.add(t, forMode: .common)
            }
        } onError: {  [weak self] error in
            self?.sendLoading=false
            if(error is NetError){
                self?.homeVM?.showToast(text: "网络错误，请检查网络")
                return
            }
            self?.homeVM?.showToast(text: error.localizedDescription)
        }
    }
    
    func loginWithVerifyCode(phone:String,code:String) -> Void {
        if !agreePrivacy {
            self.homeVM?.showToast(text: "请先同意用户和隐私协议")
            return
        }
        
        if phone.isEmpty{
            self.homeVM?.showToast(text: "请先输入电话号码")
            return
        }
        if  phone.count != 11{
            self.homeVM?.showToast(text: "电话号码错误")
            return
        }
        
        if code.isEmpty || code.count != 6{
            self.homeVM?.showToast(text: "验证码错误")
            return
        }
        
        let pushToken = UserDefaults.standard.string(forKey: SAVE_KEY_PUSH_TOKEN) ?? ""
        
        let deviceId = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
        let deviceName = UIDevice.current.name
        
    
        loginLoading=true
        let req = VerifyCodeLoginReq(phone: phone, code: code, deviceId: deviceId, deviceName: deviceName, pushToken: pushToken)
        self.provider.rx.http(.verifyCodeLogn(req)).subscribe { [weak self](data:BaseResp<VerifyCodeLoginResp>) in
            self?.loginLoading=false
            UserDefaults.standard.set(data.data?.deviceId, forKey: SAVE_KEY_DEVICE_ID)
            UserDefaults.standard.set(data.data?.deviceToken, forKey: SAVE_KEY_DEVICE_TOKEN)
            UserDefaults.standard.set(data.data?.uid, forKey: SAVE_KEY_UID)
            if data.data?.phoneHasRegister == false {
                //手机号没有注册
                self?.phoneVerifyCode = data.data?.phoneVerifyCode ?? ""
                self?.jumpToBindPhoneView = true
            }else{
                //已经注册
//                self?.showToast(text: data.msg)
                self?.homeVM?.jumpToHome = true
                updateDeviceInfo()
            }
        } onError: {  [weak self] error in
            self?.loginLoading=false
            if(error is NetError){
                self?.homeVM?.showToast(text: "网络错误，请检查网络")
                return
            }
            self?.homeVM?.showToast(text: error.localizedDescription)
        }
    }
    func test() -> Void {
        phoneVerifyCode = "test"
        jumpToBindPhoneView = true
    }
}
