//
//  WxpMessageItemBeam.swift
//  wxpusher
//
//  Created by zjie on 2025/6/8.
//

struct WxpMessageItemBeam : Decodable,Hashable{
    let id:Int64
    let url:String
    let summary:String
    let createTime:Int64
    let name:String
    let contentType:Int
    let sourceUrl:String?
    init(id: Int64, url: String, summary: String, createTime: Int64, name: String, contentType: Int,sourceUrl:String?) {
        self.id = id
        self.url = url
        self.summary = summary
        self.createTime = createTime
        self.name = name
        self.contentType = contentType
        self.sourceUrl = sourceUrl
    }
}
