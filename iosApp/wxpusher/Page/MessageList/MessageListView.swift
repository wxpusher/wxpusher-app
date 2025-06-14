//
//  MessageListView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/18.
//

import Foundation
import SwiftUI


struct MessageListView: View {
    @ObservedObject private var vm:MessageListViewVM;
    
    @EnvironmentObject var homeVM:HomeViewVM
    
    private var dateFormatter:DateFormatter;
    @State private var loaded:Bool = false
    
    
    init() {
        self.vm = MessageListViewVM()
        self.dateFormatter = DateFormatter()
        self.dateFormatter.locale =  Locale(identifier: "zh_CN")
        self.dateFormatter.setLocalizedDateFormatFromTemplate("yyyy/MM/dd HH:mm:ss")
    }
    
    
    var body: some View{
        VStack{
            List{
                if self.vm.messageList.isEmpty {
                    emptyView()
//                        .listRowSeparator(.hidden)
                }else{
                    ForEach(self.vm.messageList, id: \.self) { itemData in
                        itemView(vm:self.vm, item: itemData)
                            .onAppear(){
                                if !vm.noMore && itemData.id == vm.lastMessageId {
                                    vm.loadMore()
                                }
                            }
                        
                    }
                }
                
                if vm.noMore {
                    Text("没有更多数据了")
                        .foregroundColor(Color.defFontSecondColor)
                        .frame(maxWidth: .infinity)
//                        .listRowSeparator(.hidden)
                }
                
                if vm.footerRefreshing {
                    Text("加载中...").foregroundColor(Color.defFontSecondColor)
                        .frame(maxWidth: .infinity)
//                        .listRowSeparator(.hidden)
                }
                
            }
//            .listRowSeparator(.visible)
//            .listStyle(PlainListStyle())
//            .refreshable {
//                vm.refresh()
//            }
            .onAppear(){
                vm.setHomeVM(homeVM: self.homeVM)
                if(!loaded){
                    vm.loadMore()
                    loaded = true
                }
            }
            
        }
    }
    
    
   
    func itemView(vm:MessageListViewVM, item:MessageListItem) -> some View{
        let url = item.url?.trimmingCharacters(in: CharacterSet.whitespaces);
        return Button {
            self.homeVM.jumpToWeb(url: "http://wxpusher.zjiecode.com/api/message/"+(item.qid))
        } label: {
            VStack(alignment: .leading){
                Text(item.summary.replacingOccurrences(of: "\n", with: " "))
                    .font(.headline)
                    .lineLimit(2)
                    .foregroundColor(Color.defFontPrimaryColor)
                    .padding(.bottom,2)
                HStack{
                    Text("来源：")
                        .font(.subheadline)
                        .foregroundColor(Color.defFontSecondColor)
                    Text(item.name)
                        .font(.subheadline)
                        .foregroundColor(Color.defFontSecondColor)
                    Spacer()
                }
                HStack{
                    Text("时间：")
                        .font(.subheadline)
                        .foregroundColor(Color.defFontSecondColor)
                    Text(self.dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(item.createTime/1000))))
                        .font(.subheadline)
                        .foregroundColor(Color.defFontSecondColor)
                    Spacer()
                    if (url != nil && url?.isEmpty != true){
                        Button {
                            self.homeVM.jumpToWeb(url: url!)
                        } label: {
                            Text("链接")
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical,3)
        }
    }
    
}



func emptyView() -> some View {
    VStack{
        Image(systemName: "tray")
            .resizable()
            .foregroundColor(Color.defFontThirdColor)
            .frame(width: 100,height: 100)
            .padding(.top,100)
        Text("暂无消息")
            .foregroundColor(Color.defFontThirdColor)
            .padding(.top,10)
//        Text("去体验发送消息")
//            .font(.headline)
//            .frame(minWidth: 0,maxWidth: .infinity)
//            .foregroundColor(.white)
//            .padding(8)
//            .background(Color.accentColor)
//            .cornerRadius(4)
//            .padding()
//            .onTapGesture {
//                print("去体验发送消息")
//            }
    }
    .frame(maxWidth: .infinity)
}

struct MessageListView_Previews: PreviewProvider {
    static var previews: some View {
        MessageListView()
    }
}
