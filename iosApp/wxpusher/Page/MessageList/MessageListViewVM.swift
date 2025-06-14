//
//  MessageListViewVM.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/18.
//


import SwiftUI
import Combine
import Moya
import RxSwift

class MessageListViewVM: BaseVM {
    
    @Published var messageList:[MessageListItem] = []
    @Published var headerRefreshing: Bool = false // 用于表示是否正在刷新
    @Published var footerRefreshing: Bool = false // 用于表示是否正在加载更多数据
    @Published var noMore: Bool = false // 用于表示是否正在加载更多数据
    @Published var lastMessageId: Int64 = Int64.max
    
    private let provider = MoyaProvider<WxPusherApi>(plugins: [NetworkLoggerPlugin()])
    
    func loadMore(){
        if(self.headerRefreshing){
            return
        }
        //检查没有deviceToken，就先去登录
        let deviceToken = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_TOKEN)
        if  deviceToken==nil || deviceToken?.isEmpty == true {
            self.homeVM?.jumpToLogin = true
            return
        }
        self.loadMessageList()
    }
    
    func refresh(){
        if(self.footerRefreshing){
            return
        }
        self.noMore=false
        self.lastMessageId=Int64.max
        self.loadMessageList()
    }
    
    func loadMessageList() -> Void {
        self.provider.rx.http(.messageList(lastMessageId)).subscribe { [weak self](data:BaseResp<[MessageListItem]>) in
            var hisData = self?.messageList ?? [ ]
            let newData = data.data ??  [ ]
            if (self?.lastMessageId == Int64.max ){
                //刷新的时候清空老数据
                hisData = []
            }else if(newData.isEmpty){
                //不是刷新，但是没有拉到数据， 就说明没有了
                self?.noMore = true
            }
            if !newData.isEmpty {
                self?.lastMessageId = newData.last?.id ?? Int64.max
            }
            self?.messageList =  hisData + newData
            self?.headerRefreshing = false
            self?.footerRefreshing = false
        } onError: {  [weak self] error in
            self?.headerRefreshing = false
            self?.footerRefreshing = false
            if let netError = error as? NetError{
                //身份过期，需要先登录
                if(netError.code == 1002){
                    self?.homeVM?.jumpToLogin = true
                    return
                }
            }
            print("获取消息列表页面错误,e=" + error.localizedDescription)
        }
    }
    
}
