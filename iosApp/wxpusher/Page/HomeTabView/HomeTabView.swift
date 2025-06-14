//
//  HomeView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/7/12.
//

import SwiftUI

struct HomeTabView: View {
    private var messageListView:MessageListView;
    private var profileView:ProfileView;
    
    init() {
        messageListView = MessageListView()
        profileView = ProfileView()
    }
    
    var body: some View {
        TabView {
            messageListView.tabItem {
                Image(systemName: "paperplane")
                Text("消息列表")
            }
            
            profileView.tabItem {
                Image(systemName: "person")
                Text("我的")
            }
        }
        .navigationBarTitle("WxPusher消息推送平台", displayMode: .inline)
        .navigationBarBackButtonHidden(true)
    }
}
