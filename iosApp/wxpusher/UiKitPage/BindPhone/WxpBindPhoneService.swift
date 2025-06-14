//
//  WxpLoginService.swift
//  WxPusher-iOS
//
//  Created by zjie on 2025/6/8.
//

import Foundation
import Moya
import RxSwift
import Combine
import Toaster

class WxpBindPhoneService {
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    private let disposeBag = DisposeBag()
    
    func loginWithVerifyCode(phone:String,code:String, completion: @escaping (Bool) -> Void) -> Void {
        if phone.isEmpty{
            Toast(text: "电话号码不能为空").show()
            completion(false)
            return
        }
        if code.isEmpty || code.count != 6{
            Toast(text: "验证码错误").show()
            completion(false)
            return
        }
      
        
        let deviceId = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
        let pushToken = UserDefaults.standard.string(forKey: SAVE_KEY_PUSH_TOKEN) ?? ""
        let deviceName = UIDevice.current.name
        
        let req = VerifyCodeLoginReq(phone: phone, code: code, deviceId: deviceId, deviceName: deviceName, pushToken: pushToken)
        
       provider.rx.http(.verifyCodeLogn(req)).subscribe { (data:BaseResp<VerifyCodeLoginResp>) in
            UserDefaults.standard.set(data.data?.deviceId, forKey: SAVE_KEY_DEVICE_ID)
            UserDefaults.standard.set(data.data?.deviceToken, forKey: SAVE_KEY_DEVICE_TOKEN)
            UserDefaults.standard.set(data.data?.uid, forKey: SAVE_KEY_UID)
            if data.data?.phoneHasRegister == false {
                //手机号没有注册
                Toast(text: "绑定未完成，请先按步骤绑定").show()
                completion(false)
            }else{
                UserDefaults.standard.set(data.data?.deviceId, forKey: SAVE_KEY_DEVICE_ID)
                UserDefaults.standard.set(data.data?.deviceToken, forKey: SAVE_KEY_DEVICE_TOKEN)
                updateDeviceInfo()
                completion(true)
            }
        } onError: { error in
            Toast(text: error.localizedDescription).show()
            completion(false)
        }
        .disposed(by: disposeBag)
    }
}
