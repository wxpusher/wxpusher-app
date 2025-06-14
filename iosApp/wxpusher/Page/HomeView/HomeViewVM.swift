//
//  HomeViewVM.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/19.
//

import SwiftUI
import Combine
import Moya
import RxSwift

class HomeViewVM: BaseVM {
    
    @Published var showTipsDialog: Bool = false
    
    @Published var jumpToHome:Bool = false
    //跳转到登录页面
    @Published var jumpToLogin:Bool = false
    //跳转到Web页面
    @Published var jumpToWeb:Bool = false
    //是否显示toast
    @Published var toastShowed:Bool = false
    //toast显示的内容
    @Published var toastText:String = ""
    
    @Published var webUrl:String = ""
    
    private var webView:WebView? = nil;
    
    func showToast(text:String){
        toastText = text;
        toastShowed = true
    }
    func setWebView(webview:WebView){
        self.webView = webview;
    }
    
    func jumpToWeb(url:String) -> Void {
        self.webUrl = url;
        self.jumpToWeb = true
        if let v = self.webView {
            v.go(url: url)
        }
    }
    
}
