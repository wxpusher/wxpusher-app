//
//  BindPhoneView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/11.
//

import SwiftUI

struct BindPhoneView: View {
    
    private var phone = ""
    private var code = ""
    private var homeView:HomeView
    private var homeVM:HomeViewVM
    
    @State var  phoneVerifyCode:String
    
    @ObservedObject private var vm:BindPhoneViewVM;
    
    
    init(phone: String ,code: String,phoneVerifyCode: String,homeVM:HomeViewVM) {
        self.phone = phone
        self.code = code
        self.phoneVerifyCode = phoneVerifyCode
        vm = BindPhoneViewVM()
        self.homeVM = homeVM
        self.homeView = HomeView()
        vm.setHomeVM(homeVM: homeVM)
        
    }
    
    
    var body: some View {
        ZStack{
            ScrollView(){
                VStack{
                    Text("你好，你的手机号没有注册,你需要先和已经存在的UID绑定才可以接收消息，绑定方式如下：").foregroundColor(Color.defFontPrimaryColor).padding(.bottom,20)
                    
                    title(title: "第一步")
                    HStack{
                        Text("复制下面的绑定码").foregroundColor(Color.defFontPrimaryColor)
                        Spacer()
                    }
                    
                    HStack{
                        TextField("绑定码", text: $phoneVerifyCode)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .padding(.trailing,8)
                            .disabled(true)
                        
                        Button("复制绑定码") {
                            UIPasteboard.general.string = phoneVerifyCode
                            homeVM.showToast(text: "复制成功")
                        }
                    }
                    title(title: "第二步")
                        .padding(.top,12)
                    
                    Text("关注微信公众号WxPusher，将复制的绑定码发给公众号")
                    
                    title(title: "第三步")
                        .padding(.top,12)
                    
                    Text("发送绑定码后，请点击下面的按钮，查询绑定状态")
                    Button(action: {
                        vm.loginWithVerifyCode(phone: phone, code: code)
                    }) {
                        HStack{
                            if vm.loginLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
                                    .padding(.trailing,8)
                            }
                            Text("查询绑定状态")
                                .font(.headline)
                                .foregroundColor(.white)
                                .cornerRadius(4)
                        }.frame(minWidth: 0,maxWidth: .infinity)
                            .padding(8)
                            .background(Color.accentColor)
                            .cornerRadius(4)
                    }.padding(.top,12)
                    
                    
                    
                    Spacer()
                }.padding()
            }
        }
        .navigationTitle("UID绑定")
        .navigationBarTitleDisplayMode(NavigationBarItem.TitleDisplayMode.inline)
        .navigationBarBackButtonHidden(true)
        .onAppear(){
            vm.setHomeVM(homeVM: self.homeVM)
        }
        
    }
    
    func title(title:String) -> some View  {
        return HStack{
            Rectangle().frame(width: 5,height: 20)
                .foregroundColor(Color.defAccentPrimaryColor)
            Text(title)
                .font(.system(size: 20))
                .foregroundColor(Color.defAccentPrimaryColor)
            Spacer()
        }
    }
}

//struct BindPhoneView_Previews: PreviewProvider {
//    @EnvironmentObject private var homeVM:HomeViewVM
//    static var previews: some View {
////        BindPhoneView(phone: "", code: "", phoneVerifyCode: "",homeVM: homeVM)
//    }
//}
