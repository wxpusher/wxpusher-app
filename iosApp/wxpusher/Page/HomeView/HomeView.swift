//
//  ContentView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/7/8.
//

import SwiftUI

import SwiftUI

struct HomeView: View {
    @ObservedObject private var vm:HomeViewVM = HomeViewVM()
    
    private var messageListView:MessageListView;
    private var profileView:ProfileView;
    private var loginView:LoginView;
    private var webViewModel: WebViewModel;
    private var webView:WebView;
    private var webViewWithActionSheet:WebViewWithActionSheet;
    private var homeTabView:HomeTabView;
    
    init() {
        messageListView = MessageListView()
        profileView = ProfileView()
        loginView = LoginView()
        homeTabView = HomeTabView()
        webViewModel = WebViewModel()
        webView = WebView(viewModel: webViewModel)
        webViewWithActionSheet = WebViewWithActionSheet(webView: webView)
        self.vm.setWebView(webview: webView)
    }
    
    var body: some View {
        NavigationView{
            ZStack{
                NavigationLink("", destination:loginView, isActive: $vm.jumpToLogin)
                if !self.vm.webUrl.isEmpty{
                    NavigationLink("", destination: self.webViewWithActionSheet, isActive: $vm.jumpToWeb)
                }
                NavigationLink("", destination:homeTabView, isActive: $vm.jumpToHome)
                homeTabView
            }
            .navigationBarTitle("WxPusher消息推送平台", displayMode: .inline)
            .navigationBarBackButtonHidden(true)
        }
        .navigationViewStyle(.stack)
        .popup(isPresented: $vm.toastShowed,position:.top, autohideIn: 3) {
            Text(vm.toastText)
                .foregroundColor(Color.white)
                .padding(10)
                .background(Color.gray)
                .cornerRadius(5.0)
                .padding(10)
                .background(Color.white.opacity(0))
            
        }
        .environmentObject(self.vm)
        .onReceive(NotificationCenter.default.publisher(for:notiKey)) { notification in
            //点击远程通知，打开消息详情页面
            if let userInfo = notification.userInfo {
                self.vm.jumpToWeb(url: userInfo["messageUrl"] as! String)
                self.webView.go(url: userInfo["messageUrl"] as! String)
            }
        }
        .onAppear(){
            reqNotePermission()
        }
        .alert(isPresented: $vm.showTipsDialog) {
            Alert(
                title: Text("异常提示"),
                message: Text("暂无通知权限，你可能无法收到通知，请前往设置-WxPusher-通知-打开允许通知。"),
                dismissButton: .default(Text("我知道了")) {
                    
                }
                
            )
        }
        
    }
    
    func reqNotePermission()->Void{
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .authorized {
                return
            }
            //没有权限，就申请通知权限
            UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
                    if granted {
                        //用户许可通知权限，注册远程通知
                        dispatch_sync_safely_main_queue{
                            UIApplication.shared.registerForRemoteNotifications()
                        }
                    }else{
                        //如果没有deviceToken，说明没有登录，不进行异常提示
                        let deviceToken = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_TOKEN)
                        if  deviceToken==nil || deviceToken?.isEmpty == true {
                            return
                        }
                        //用户拒绝通知权限，显示请求通知权限的提示
                        vm.showTipsDialog = true;
                    }
                }
        }
        
        
    }
}
