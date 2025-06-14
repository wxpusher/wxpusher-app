//
//  BindPhoneViewView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/11.
//
//
import SwiftUI
import Combine
import Moya
import RxSwift

class BindPhoneViewVM: BaseVM {
    
    @Published var loginLoading:Bool = false
    
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    
    func loginWithVerifyCode(phone:String,code:String) -> Void {
        if phone.isEmpty || phone.count != 11{
            self.homeVM?.showToast(text: "电话号码错误")
            return
        }
        if code.isEmpty || code.count != 6{
            self.homeVM?.showToast(text: "验证码错误")
            return
        }
        loginLoading=true
        
        let deviceId = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
        let pushToken = UserDefaults.standard.string(forKey: SAVE_KEY_PUSH_TOKEN) ?? ""
        let deviceName = UIDevice.current.name
        
        let req = VerifyCodeLoginReq(phone: phone, code: code, deviceId: deviceId, deviceName: deviceName, pushToken: pushToken)
        print("req=\(req)")
        self.provider.rx.http(.verifyCodeLogn(req)).subscribe { [weak self](data:BaseResp<VerifyCodeLoginResp>) in
            self?.loginLoading=false
            UserDefaults.standard.set(data.data?.deviceId, forKey: SAVE_KEY_DEVICE_ID)
            UserDefaults.standard.set(data.data?.deviceToken, forKey: SAVE_KEY_DEVICE_TOKEN)
            UserDefaults.standard.set(data.data?.uid, forKey: SAVE_KEY_UID)
            if data.data?.phoneHasRegister == false {
                //手机号没有注册
                self?.homeVM?.showToast(text: "绑定未完成，请先按步骤绑定")
            }else{
                UserDefaults.standard.set(data.data?.deviceId, forKey: SAVE_KEY_DEVICE_ID)
                UserDefaults.standard.set(data.data?.deviceToken, forKey: SAVE_KEY_DEVICE_TOKEN)
                self?.homeVM?.jumpToHome = true
                updateDeviceInfo()
            }
        } onError: {  [weak self] error in
            self?.loginLoading=false
            self?.homeVM?.showToast(text: error.localizedDescription)
        }
    }
    
}
