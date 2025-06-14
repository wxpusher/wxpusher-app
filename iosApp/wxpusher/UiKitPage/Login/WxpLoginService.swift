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

class WxpLoginService {
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    private let disposeBag = DisposeBag()
    
    func login(phone: String, completion: @escaping (Bool,String) -> Void) {
        if phone.isEmpty {
            completion(false, "手机号不能为空")
            return
        }
        provider.rx.http(.sendVerify(SendVerifyReq(phone: phone))).subscribe {(data:BaseResp<Bool>) in
            completion(true,"")
        } onError: { error in
            if let netError = error as? NetError {
                completion(false, netError.msg ?? "网络错误")
            } else {
                completion(false, error.localizedDescription)
            }
        }
        .disposed(by: disposeBag)
    }
    
    func verifyCodeLogin(phone: String,code :String,completion: @escaping (VerifyCodeLoginResp?,String) -> Void){
        let pushToken = UserDefaults.standard.string(forKey: SAVE_KEY_PUSH_TOKEN) ?? ""
        
        let deviceId = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
        let deviceName = UIDevice.current.name
        
    
        let req = VerifyCodeLoginReq(phone: phone, code: code, deviceId: deviceId, deviceName: deviceName, pushToken: pushToken)
        provider.rx.http(.verifyCodeLogn(req)).subscribe { [weak self](data:BaseResp<VerifyCodeLoginResp>) in
            
            UserDefaults.standard.set(data.data?.deviceId, forKey: SAVE_KEY_DEVICE_ID)
            UserDefaults.standard.set(data.data?.deviceToken, forKey: SAVE_KEY_DEVICE_TOKEN)
            UserDefaults.standard.set(data.data?.uid, forKey: SAVE_KEY_UID)
            if(data.data != nil){
                completion(data.data!,"")
            }else{
                completion(nil,"数据错误")
            }
        } onError: { error in
            if let netError = error as? NetError {
                completion(nil, netError.msg ?? "网络错误")
            } else {
                completion(nil, error.localizedDescription)
            }
        }
        .disposed(by: disposeBag)
    }
}
