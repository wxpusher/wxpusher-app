//
//  TestView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/4.
//

import SwiftUI
import Moya
import RxSwift
import ExytePopupView

struct TestView: View {
    @EnvironmentObject var homeVM:HomeViewVM
    
    @State private var resp:String = "nil"
    @State private var loading:Bool = false
    @State private var  showingPopup:Bool = false
    private var disposeBag = DisposeBag();
    var body: some View {
        VStack{
            Image(systemName: "tray")
                .resizable()
                .foregroundColor(Color.defFontThirdColor)
                .frame(width: 100,height: 100)
            Text("暂无消息")
                .foregroundColor(Color.defFontThirdColor)
                .padding(.top,10)
            Button {
                
            } label: {
                Text("去体验发送消息")
                    .font(.headline)
                    .frame(minWidth: 0,maxWidth: .infinity)
                    .foregroundColor(.white)
                    .padding(8)
                    .background(Color.accentColor)
                    .cornerRadius(4)
                    .padding()
            }
        }
        
    }
}

struct TestView_Previews: PreviewProvider {
    static var previews: some View {
        TestView()
    }
}
