//
//  LoginView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/2.
//

import SwiftUI

struct LoginView: View {
    @State private var fullName: String = ""
    
    @State private var phoneNumber = ""
    @State private var verificationCode = ""
    @State private var isCodeSent = false
    
    @State private var isSecondViewActive = false
    
    
    
    @ObservedObject private var vm:LoginViewVM;
    @EnvironmentObject var homeVM:HomeViewVM;
    
    private var webViewModel: WebViewModel;
    private var webView:WebView;
    private var webViewWithActionSheet:WebViewWithActionSheet;
    
    init() {
        vm = LoginViewVM();
        webViewModel = WebViewModel();
        webView = WebView(viewModel: webViewModel);
        webViewWithActionSheet = WebViewWithActionSheet(webView: webView);
    }
    
    var body: some View {
        ZStack{
            NavigationLink("", destination: BindPhoneView(phone: phoneNumber, code: verificationCode, phoneVerifyCode: vm.phoneVerifyCode,homeVM: homeVM), isActive: $vm.jumpToBindPhoneView)
                .frame(height: 0)
            if self.vm.showWebView {
                NavigationLink("", destination: self.webViewWithActionSheet, isActive: $vm.showWebView)
            }
            
            VStack {
//                Spacer()
//                Text("用户登录")
//                    .font(.largeTitle)
//                    .padding(.bottom,4)
//                Text("首次登录自动注册")
//                    .font(.caption)
//                    .foregroundColor(.gray)
//                
//                
//                TextField("手机号", text: $phoneNumber)
//                    .textFieldStyle(RoundedBorderTextFieldStyle())
//                    .padding(.top,24)
//                    .keyboardType(UIKeyboardType.numberPad)
//                    .submitLabel(SubmitLabel.done)
//                
//                
//                HStack(alignment: .center){
//                    TextField("验证码", text: $verificationCode)
//                        .textFieldStyle(RoundedBorderTextFieldStyle())
//                        .keyboardType(UIKeyboardType.numberPad)
//                        .submitLabel(SubmitLabel.done)
//                        .padding(.trailing,12)
//                    Button(action: {
//                        vm.sendVerifyCode(phone: phoneNumber)
//                        
//                    }) {
//                        HStack{
//                            if vm.sendLoading {
//                                ProgressView()
//                                    .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
//                                    .padding(.trailing,8)
//                            }
//                            
//                            Text((vm.restSecond>0) ? "请\(vm.restSecond)秒后重试":"获取验证码")
//                                .font(.headline)
//                                .foregroundColor(.white)
//                                .disabled(vm.restSecond>0)
//                        }.frame(width: 130)
//                            .padding(8)
//                            .background(Color.accentColor)
//                            .cornerRadius(4)
//                    }
//                    
//                }.padding(.top,12)
//                
//                Button(action: {
//                    login()
//                }) {
//                    HStack{
//                        if vm.loginLoading {
//                            ProgressView()
//                                .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
//                                .padding(.trailing,8)
//                        }
//                        Text("登录")
//                            .font(.headline)
//                            .foregroundColor(.white)
//                            .cornerRadius(4)
//                        
//                    }.frame(minWidth: 0,maxWidth: .infinity)
//                        .padding(8)
//                        .background(Color.accentColor)
//                        .cornerRadius(4)
//                }.padding(.top,12)
//                
//                HStack(alignment: .center){
//                    Toggle("", isOn: $vm.agreePrivacy)
//                        .toggleStyle(CheckBoxToggleStyle(shape: .circle))
//                    Text("同意《").foregroundColor(Color.defFontPrimaryColor)
//                    Button {
//                        vm.agreePrivacy = !vm.agreePrivacy
//                    } label: {
//                        Button {
//                            vm.setShowWebView(show: true)
//                            self.webView.go(url: "https://wxpusher.zjiecode.com/admin/agreement/index-argeement.html")
//                        } label: {
//                            Text("隐私协议和用户协议")
//                        }
//                    }
//                    Text("》")
//                        .foregroundColor(Color.defFontPrimaryColor)
//                    Spacer()
//                }
//                .padding(.top,12)
//                Spacer()
//                Spacer()
//                Text("© 2023 WxPusher")
//                    .font(.footnote)
//                    .foregroundColor(.gray)
                
            }
            .padding(24)
            
        }
        .navigationTitle("用户登录")
        .navigationBarTitleDisplayMode(NavigationBarItem.TitleDisplayMode.inline)
        .navigationBarHidden(true)
        .navigationBarBackButtonHidden(true)
        .onAppear(){
            vm.setHomeVM(homeVM: self.homeVM)
        };
        
    }
    
    func login() -> Void {
        vm.loginWithVerifyCode(phone: phoneNumber, code: verificationCode)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
