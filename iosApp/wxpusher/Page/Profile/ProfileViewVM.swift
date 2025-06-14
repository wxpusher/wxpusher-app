//
//  ProfileViewVM.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/27.
//

import SwiftUI
import Combine
import Moya
import RxSwift

class ProfileViewVM: BaseVM {
    
    @Published var showInputDialog: Bool = false
    @Published var inputText: String = ""
    
    private let disposeBag = DisposeBag();
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    
    //正在退出登录
    @Published var logouting: Bool = false
    
    func logout() -> Void {
        self.logouting = true
        self.provider.rx.http(.logout).subscribe { [weak self](data:BaseResp<Bool>) in
            self?.logouting = false
            self?.homeVM?.jumpToLogin = true
        } onError: {  [weak self] error in
            self?.logouting = false
            self?.homeVM?.showToast(text: error.localizedDescription)
            print("退出登录错误,e=" + error.localizedDescription)
        }.disposed(by: disposeBag)
    }
    
    //解绑
    func unbind() -> Void {
        self.provider.rx.http(.unbind).subscribe { [weak self](data:BaseResp<Bool>) in
            self?.homeVM?.jumpToLogin = true
        } onError: {  [weak self] error in
            self?.homeVM?.showToast(text: error.localizedDescription)
            print("注销错误,e=" + error.localizedDescription)
        }.disposed(by: disposeBag)
    }
}
