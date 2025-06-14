//
//  BaseVM.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/19.
//

import SwiftUI
import Combine
import Moya
import RxSwift

class BaseVM: ObservableObject {
    var homeVM:HomeViewVM?
 
    func setHomeVM(homeVM:HomeViewVM){
        self.homeVM = homeVM;
    }
}
