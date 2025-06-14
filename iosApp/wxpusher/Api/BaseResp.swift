//
//  BaseResp.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/28.
//

import Foundation

public struct BaseResp<T: Decodable>: Decodable {
    let code:Int;
    
    let msg:String;
    
    let data: T?;
}
