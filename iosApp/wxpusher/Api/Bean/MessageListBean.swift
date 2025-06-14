//
//  MessageListBean.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/18.
//

//验证码登录请求
struct MessageListItem : Decodable,Hashable{
    let id:Int64
    let qid:String
    let summary:String
    let createTime:Int64
    let name:String
    let contentType:Int
    let url:String?
    init(id: Int64, qid: String, summary: String, createTime: Int64, name: String, contentType: Int,url:String?) {
        self.id = id
        self.qid = qid
        self.summary = summary
        self.createTime = createTime
        self.name = name
        self.contentType = contentType
        self.url = url
    }
}
