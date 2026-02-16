//
//  WxpBaseMvpUIViewController.swift
//  WxPusher-iOS
//
//  Created by zjie on 2025/6/15.
//

import Foundation
import UIKit
import shared


class WxpBaseMvpUIViewController<P: IWxpBaseMvpPresenter>: UIViewController {
    
    // Presenter 实例
    var presenter: P!
    
    // 创建 Presenter 的方法 - 子类必须实现
    // 这里主要是为了和IVew匹配，所以返回 Any? 后面强制转型一下
    func createPresenter() -> Any? {
        fatalError("Must override createPresenter()")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        presenter.onShow()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        presenter = (createPresenter() as! P)
        
    }
    
    deinit {
        if(presenter != nil){
            presenter.onDestroy()
        }
    }
}
