//
//  SwiftUIView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/26.
//

import SwiftUI

struct ProfileView: View {
    @ObservedObject private var vm:ProfileViewVM = ProfileViewVM()
    
    @EnvironmentObject var homeVM:HomeViewVM
    
    var body: some View {
        VStack(alignment: .center){
            List{
                VStack{
                    Image("Logo")
                        .resizable()
                        .frame(width: 160,height: 160)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                        .shadow(radius: 5)
                    Text("WxPusher-iOS")
                        .foregroundColor(Color.defFontPrimaryColor)
                        .padding(.top,10)
                    Text(version())
                        .foregroundColor(Color.defFontSecondColor)
                }
                .padding(.top,20)
                .frame(maxWidth: .infinity)
//                .listRowSeparator(.hidden)
                
                HStack{
                    Text("官方公众号")
                    Spacer()
                    Button {
                        UIPasteboard.general.string = "WxPusher"
                        homeVM.showToast(text:"复制成功")
                    } label: {
                        Text("WxPusher")
                            .foregroundColor(Color.defFontSecondColor)
                    }
                }
                HStack{
                    Text("设备ID")
                    Spacer()
                    Button {
                        UIPasteboard.general.string = UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? ""
                        homeVM.showToast(text: "复制成功")
                    } label: {
                        Text(UserDefaults.standard.string(forKey: SAVE_KEY_DEVICE_ID) ?? "")
                            .foregroundColor(Color.defFontSecondColor)
                            .truncationMode(Text.TruncationMode.middle)
                            .lineLimit(1)
                    }

                   
                }
                HStack{
                    Text("推送UID")
                    Spacer()
                    Button {
                        UIPasteboard.general.string = UserDefaults.standard.string(forKey: SAVE_KEY_UID) ?? ""
                        homeVM.showToast(text: "复制成功")
                    } label: {
                        Text(UserDefaults.standard.string(forKey: SAVE_KEY_UID) ?? "")
                            .foregroundColor(Color.defFontSecondColor)
                            .truncationMode(Text.TruncationMode.middle)
                            .lineLimit(1)
                    }

                   
                }
                
                HStack{
                    Text("软件版本")
                    Spacer()
                    Text(version())
                        .foregroundColor(Color.defFontSecondColor)
                }
                
                HStack{
                    Text("用户数据")
                    Spacer()
                    Button {
                        vm.showInputDialog = true
                    } label: {
                        Text("注销账户")
                            .foregroundColor(Color.defFontSecondColor)
                            .truncationMode(Text.TruncationMode.middle)
                            .lineLimit(1)
                    }

                }
                Button(action: {
                    vm.logout()
//                    sendNotification(title:"标题",subTitle:"子标题")
                }) {
                    HStack{
                        if vm.logouting {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: Color.white))
                                .padding(.trailing,8)
                        }
                        Text("退出登录")
                            .font(.headline)
                            .foregroundColor(.white)
                        
                    }
                    .frame(minWidth: 0,maxWidth: .infinity)
                    .padding(12)
                    .background(Color.accentColor)
                    .cornerRadius(4)
                }
//                .listRowSeparator(.hidden)
            }
            .listStyle(PlainListStyle())
        }
        .onAppear(){
            vm.setHomeVM(homeVM: homeVM)
        }
        .sheet(isPresented: $vm.showInputDialog) {
            InputSheet(inputText: $vm.inputText, showingSheet: $vm.showInputDialog,vm: vm)
        }
    }
    
   
}
struct InputSheet: View {
    @Binding var inputText: String
    @Binding var showingSheet: Bool
    @State var tips:String = ""
    
    var vm:ProfileViewVM
    
    var body: some View {
        VStack(spacing: 20) {
            Text("注销账户提醒")
                .foregroundColor(.red)
                .font(.title)
            
            Text("如果你不再使用本账户，你可以注销当前手机号信息，注销后，当前手机号将释放，后续可以再使用本手机号绑定新账户，请问是否注销？")
                .foregroundColor(Color.defFontPrimaryColor)
            
            if(tips.count>0){
                Text(tips)
                    .padding(EdgeInsets(top: 20, leading: 0, bottom: 0, trailing: 0))
                    .foregroundColor(.red)
            }else{
                Text("注销账户，请在下方输入\"注销\"。")
                    .padding(EdgeInsets(top: 20, leading: 0, bottom: 0, trailing: 0))
                    .foregroundColor(Color.accentColor)
            }
            
            
            TextField("请输入：注销", text: $inputText)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding()
            
            
            
            HStack {
                Button("取消") {
                    showingSheet = false
                }
                .padding()
                
                Button("注销账户") {
                    if(inputText != "注销"){
                        tips = "请输入「注销」以确认操作"
                        return
                    }
                    showingSheet = false
                    vm.unbind()
                }
                .padding()
            }
        }
        .padding()
    }
}

struct SwiftUIView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
    }
}
