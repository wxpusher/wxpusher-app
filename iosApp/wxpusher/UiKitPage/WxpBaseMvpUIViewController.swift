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
    func createPresenter() -> P {
        fatalError("Must override createPresenter()")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        presenter.onShow()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        presenter = createPresenter()
        
    }
    
    deinit {
        presenter.onDestroy()
    }
}
