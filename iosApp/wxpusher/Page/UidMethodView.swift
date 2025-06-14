//
//  UidMethodView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/3.
//

import SwiftUI

struct UidMethodView: View {
    @State private var type = ""
    @State private var uid = ""
//
//    private let subTitleColor = Color.init(hex: "#666666",opacity: 1)
//    private let selectBg = Color.init(hex: "#007AFF",opacity: 0.2)
//    private let selectBorder = Color.init(hex: "#007AFF",opacity: 1)
//
    var body: some View {
        VStack{
            Button {
                type="bind"
            } label: {
                VStack(alignment: .leading) {
                    HStack{
                        Text("绑定公众号UID")
                            .foregroundColor(.black)
                            .multilineTextAlignment(.leading)
                        Spacer()
                    }

                    Text("把iOS客户端账号和公众号账号进行绑定，绑定后共用同一个UID，双端接收消息。")
                        .foregroundColor(Color("SecoundFontColor"))
                        .font(.subheadline)
                        .multilineTextAlignment(.leading)
                        .padding(EdgeInsets(top: 2, leading: 0, bottom: 0, trailing: 0))
                }

                .frame(maxWidth: .infinity)
                .padding(8)
                .background( type == "bind" ? Color.defAccentSecoundColor : Color.clear)
                .cornerRadius(4)
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(type == "bind" ? Color.accentColor : Color.gray, lineWidth: 1))

            }

            Rectangle().frame(width: 10,height: 10,alignment: .leading).foregroundColor(.clear)

            Button {
                type="create"
            } label: {
                VStack(alignment: .leading) {
                    HStack{
                        Text("创建新的UID")
                            .foregroundColor(.black)
                            .multilineTextAlignment(.leading)
                        Spacer()
                    }

                    Text("iOS客户端作为一个独立账号，不和公众号绑定，无法接收公众号的消息。")
                        .foregroundColor(Color.defFontSecondColor)
                        .font(.subheadline)
                        .multilineTextAlignment(.leading)
                        .padding(EdgeInsets(top: 2, leading: 0, bottom: 0, trailing: 0))
                }

                .frame(maxWidth: .infinity)
                .padding(8)
                .background( type == "create" ? Color.defAccentSecoundColor : Color.clear)
                .cornerRadius(4)
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(type == "create" ? Color.defAccentSecoundColor : Color.gray, lineWidth: 1))
            }

            Spacer()
            if type == "bind"{
                Text("请先关注公众号：WxPusher，在菜单-我的-我的UID获取到你的用户UID")
                    .font(.system(size: 14))

                TextField("请输入UID", text: $uid)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(8)

                Rectangle().frame(width: 10,height:1,alignment: .leading).foregroundColor(.clear)

            }
            if type == "create"{
                Text("暂不支持，功能开发中")
                    .font(.system(size: 14))


                Rectangle().frame(width: 10,height:1,alignment: .leading).foregroundColor(.clear)

            }
            Button(action: {

            }) {
                Text("确定")
                    .font(.headline)
                    .frame(minWidth: 0,maxWidth: .infinity)
                    .padding(8)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(4)
            }

        }
        .padding()
        
    }
}

struct UidMethodView_Previews: PreviewProvider {
    static var previews: some View {
        UidMethodView()
    }
}
