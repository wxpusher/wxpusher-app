//
//  NetError.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/5.
//

import Swift
import Foundation

public class NetError:Swift.Error,LocalizedError{
    
    public var code:Int?;
    public var msg:String?;
    public var cause:Error?;
    
    public var errorDescription: String?{
        if let m = self.msg {
            return m
        }
        if let c = self.cause{
            return c.localizedDescription
        }
        return "未知错误"
    }
    
    init(code: Int, msg: String) {
        self.code = code
        self.msg = msg
    }
    init(cause:Error) {
        self.cause = cause
    }
    init(code: Int, msg: String,cause:Error) {
        self.code = code
        self.msg = msg
        self.cause = cause
    }
    
}
