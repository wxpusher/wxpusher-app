//
//  MessageHistoryView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/5.
//

import SwiftUI

struct MessageHistoryView: View {
    let data = (1...1000).map { "Item \($0)" }
    var body: some View {
        List {
            LazyVStack {
                ForEach(data, id: \.self) { item in
                    itemView(item: item)
                }
            }
           
        } .listStyle(PlainListStyle())
            
    }
}

func itemView(item:Any) -> some View{
    return ZStack {
        RoundedRectangle(cornerRadius: 10) // 圆角矩形作为卡片的背景
            .fill(Color.white) // 背景颜色为白色
            .frame(width: .infinity, height: 150) // 设置卡片的宽度和高度
            .shadow(color: Color.gray, radius: 3, x: 1, y: 1) // 添加阴影效果
        
        Text("This is a Card") // 卡片中的文本内容
            .font(.headline)
            .foregroundColor(Color.black)
    }
}

struct MessageHistoryView_Previews: PreviewProvider {
    static var previews: some View {
        MessageHistoryView()
    }
}
