//
//  CheckBoxToggleStyle.swift
//  wxpusher
//
//  Created by 张杰 on 2023/10/24.
//

import SwiftUI
struct CheckBoxToggleStyle: ToggleStyle{
    
        enum CheckBoxShape: String{
            case circle
            case square
        }
        let shape : CheckBoxShape
        init(shape: CheckBoxShape = .circle){
            self.shape = shape
        }
        //configuration中包含isOn和label
        func makeBody(configuration: Configuration) -> some View {
            let systemName:String = configuration.isOn ? "checkmark.\(shape.rawValue).fill" : shape.rawValue
            Button(action: {
                configuration.isOn.toggle()
            }) {
                configuration.label
                Image(systemName: systemName)
                    .resizable()
                    .frame(width: 20, height: 20)
            }
            

        }
    }
