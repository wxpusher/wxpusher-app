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

class WxpMessageListService {
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    private let disposeBag = DisposeBag()
    private var lastMessageId: Int64 = Int64.max
    private var messageList: [WxpMessageItemBeam] = []
    private var hasMore = true
    
    func loadData(completion: @escaping ([WxpMessageItemBeam],Bool,String?,Bool) -> Void){
        provider.rx.http(.messageList(lastMessageId)).subscribe { [weak self](data:BaseResp<[WxpMessageItemBeam]>) in
                var hisData = self?.messageList ?? []
                let newData = data.data ??  [ ]
                
                if (self?.lastMessageId == Int64.max ){
                    //刷新的时候清空老数据
                    hisData = []
                }else if(newData.isEmpty){
                    //不是刷新，但是没有拉到数据， 就说明没有了
                    self?.hasMore = false
                }
                self?.messageList =  hisData + newData
            completion(self?.messageList ?? [],self?.hasMore ?? true,nil,false)
            } onError: {  [weak self] error in
                if let netError = error as? NetError{
                    //身份过期，需要先登录
                    if(netError.code == WxpNetworkConstants.WxpNetworkConstantsNeedLogin){
                        completion(self?.messageList ?? [],self?.hasMore  ?? true, error.localizedDescription,true)
                        return
                    }
                }
                completion(self?.messageList ?? [],self?.hasMore  ?? true, error.localizedDescription,false)
                print("获取消息列表页面错误,e=" + error.localizedDescription)
            }
            .disposed(by: disposeBag)
    }
    
}
